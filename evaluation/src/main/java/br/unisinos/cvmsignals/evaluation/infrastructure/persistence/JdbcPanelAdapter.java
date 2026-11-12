package br.unisinos.cvmsignals.evaluation.infrastructure.persistence;

import br.unisinos.cvmsignals.evaluation.application.port.PanelViewPort;
import br.unisinos.cvmsignals.evaluation.application.port.TickerMappingPort;
import br.unisinos.cvmsignals.evaluation.application.port.TickerMappingPort.TickerEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Monta o painel em duas consultas planas — sinais e cotações — e faz o
 * agrupamento em memória. O volume é o de um estudo com poucas dezenas de
 * documentos, então agregar aqui sai mais simples e legível do que
 * construir o aninhamento em SQL.
 */
@Component
public class JdbcPanelAdapter implements PanelViewPort, TickerMappingPort {

    /** Uma linha por sinal, já com empresa, documento e ticker resolvidos. */
    private static final String SIGNALS = """
            SELECT e.cnpj, e.nome,
                   -- O mapa manda: empresas ingeridas depois da migration V2
                   -- entram com empresa.ticker nulo, e sem este COALESCE
                   -- apareceriam sem código B3 e sem cotação.
                   COALESCE(m.ticker, e.ticker) AS ticker, m.origem AS ticker_origem,
                   d.id AS documento_id, d.tipo, d.data_referencia,
                   d.url_origem, d.protocolo,
                   s.modelo, s.tipo AS tipo_sinal, s.rotulo, s.confianca,
                   s.justificativa, s.evidencia
            FROM sinal s
            JOIN documento d ON d.id = s.documento_id
            JOIN empresa e ON e.id = d.empresa_id
            LEFT JOIN mapa_ticker m ON m.cnpj = e.cnpj
            ORDER BY e.nome, d.data_referencia DESC, d.id, s.modelo, s.tipo, s.criado_em
            """;

    /** Últimos 60 pregões de cada ticker — o bastante para o gráfico do card. */
    private static final String PRICES = """
            SELECT ticker, data, fechamento
            FROM (SELECT ticker, data, fechamento,
                         ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY data DESC) AS rn
                  FROM serie_preco) t
            WHERE rn <= 60
            ORDER BY ticker, data
            """;

    private final JdbcTemplate jdbc;

    public JdbcPanelAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Panel load() {
        Map<String, List<PricePoint>> historyByTicker = loadPriceHistory();
        Map<String, CompanyAccumulator> companies = new LinkedHashMap<>();

        jdbc.query(SIGNALS, rs -> {
            // Lidos fora do computeIfAbsent: o lambda de Function não propaga SQLException.
            String cnpj = rs.getString("cnpj");
            String nome = rs.getString("nome");
            String ticker = rs.getString("ticker");
            String tickerOrigem = rs.getString("ticker_origem");
            CompanyAccumulator company = companies.computeIfAbsent(cnpj,
                    key -> new CompanyAccumulator(key, nome == null ? "" : nome, ticker, tickerOrigem));
            company.add(
                    rs.getObject("documento_id", UUID.class),
                    rs.getString("tipo"),
                    rs.getObject("data_referencia", LocalDate.class),
                    rs.getString("url_origem"),
                    rs.getString("protocolo"),
                    rs.getString("modelo"),
                    rs.getString("tipo_sinal"),
                    new Verdict(rs.getString("rotulo"), rs.getDouble("confianca"),
                            rs.getString("justificativa"), rs.getString("evidencia")));
        });

        List<CompanyCard> cards = companies.values().stream()
                .map(c -> c.toCard(historyByTicker.get(c.ticker)))
                .toList();
        return new Panel(Instant.now(), cards);
    }

    @Override
    public void assign(String cnpj, String ticker, String referenceName) {
        String digits = cnpj.replaceAll("\\D", "");
        String code = ticker.strip().toUpperCase();
        jdbc.update("""
                INSERT INTO mapa_ticker (cnpj, ticker, nome_referencia, origem, atualizado_em)
                VALUES (?, ?, ?, 'MANUAL', now())
                ON CONFLICT (cnpj) DO UPDATE
                    SET ticker = EXCLUDED.ticker,
                        nome_referencia = EXCLUDED.nome_referencia,
                        origem = 'MANUAL',
                        atualizado_em = now()
                """, digits, code, referenceName == null ? "" : referenceName);
        jdbc.update("UPDATE empresa SET ticker = ? WHERE cnpj = ?", code, digits);
    }

    @Override
    public List<TickerEntry> list() {
        return jdbc.query("""
                SELECT m.cnpj, m.ticker, m.nome_referencia, m.origem,
                       EXISTS (SELECT 1 FROM empresa e WHERE e.cnpj = m.cnpj) AS ingerida
                FROM mapa_ticker m
                ORDER BY m.ticker
                """,
                (rs, i) -> new TickerEntry(
                        rs.getString("cnpj"),
                        rs.getString("ticker"),
                        rs.getString("nome_referencia"),
                        rs.getString("origem"),
                        rs.getBoolean("ingerida")));
    }

    private Map<String, List<PricePoint>> loadPriceHistory() {
        Map<String, List<PricePoint>> byTicker = new LinkedHashMap<>();
        jdbc.query(PRICES, rs -> {
            byTicker.computeIfAbsent(rs.getString("ticker"), k -> new ArrayList<>())
                    .add(new PricePoint(rs.getObject("data", LocalDate.class), rs.getBigDecimal("fechamento")));
        });
        return byTicker;
    }

    /** Agrupa os sinais planos de uma companhia em documentos × modelos. */
    private static final class CompanyAccumulator {
        private final String cnpj;
        private final String name;
        private final String ticker;
        private final String tickerOrigin;
        private final Map<UUID, DocumentAccumulator> documents = new LinkedHashMap<>();

        private CompanyAccumulator(String cnpj, String name, String ticker, String tickerOrigin) {
            this.cnpj = cnpj;
            this.name = name;
            this.ticker = ticker;
            this.tickerOrigin = tickerOrigin;
        }

        private void add(UUID documentId, String type, LocalDate referenceDate,
                         String sourceUrl, String protocol,
                         String model, String signalType, Verdict verdict) {
            documents.computeIfAbsent(documentId,
                            id -> new DocumentAccumulator(id, type, referenceDate, sourceUrl, protocol))
                    .add(model, signalType, verdict);
        }

        private CompanyCard toCard(List<PricePoint> history) {
            return new CompanyCard(cnpj, name, ticker, tickerOrigin, quoteFrom(ticker, history),
                    documents.values().stream().map(DocumentAccumulator::toCard).toList());
        }
    }

    /** Um documento com a leitura de cada modelo que o analisou. */
    private static final class DocumentAccumulator {
        private final UUID documentId;
        private final String type;
        private final LocalDate referenceDate;
        private final String sourceUrl;
        private final String protocol;
        private final Map<String, ReadingAccumulator> readings = new LinkedHashMap<>();

        private DocumentAccumulator(UUID documentId, String type, LocalDate referenceDate,
                                    String sourceUrl, String protocol) {
            this.documentId = documentId;
            this.type = type;
            this.referenceDate = referenceDate;
            this.sourceUrl = sourceUrl;
            this.protocol = protocol;
        }

        private void add(String model, String signalType, Verdict verdict) {
            readings.computeIfAbsent(model, ReadingAccumulator::new).add(signalType, verdict);
        }

        private DocumentCard toCard() {
            return new DocumentCard(documentId, type, referenceDate, sourceUrl, protocol,
                    readings.values().stream().map(ReadingAccumulator::toReading).toList());
        }
    }

    /**
     * Sentimento e risco são um por documento; eventos corporativos vêm em
     * lista, porque o prompt permite o modelo listar vários (ou nenhum).
     */
    private static final class ReadingAccumulator {
        private final String model;
        private Verdict sentiment;
        private Verdict risk;
        private final List<Verdict> events = new ArrayList<>();

        private ReadingAccumulator(String model) {
            this.model = model;
        }

        private void add(String signalType, Verdict verdict) {
            switch (signalType) {
                case "SENTIMENTO" -> sentiment = verdict;
                case "RISCO" -> risk = verdict;
                case "EVENTO_CORPORATIVO" -> events.add(verdict);
                default -> {
                    // tipo desconhecido: ignorado em vez de derrubar o painel
                }
            }
        }

        private ModelReading toReading() {
            return new ModelReading(model, sentiment, risk, List.copyOf(events));
        }
    }

    /** Último fechamento e variação contra o pregão anterior. */
    private static Quote quoteFrom(String ticker, List<PricePoint> history) {
        if (ticker == null || history == null || history.isEmpty()) {
            return null;
        }
        PricePoint last = history.get(history.size() - 1);
        BigDecimal previous = history.size() > 1 ? history.get(history.size() - 2).close() : null;
        Double changePct = null;
        if (previous != null && previous.signum() != 0) {
            changePct = last.close().subtract(previous)
                    .divide(previous, MathContext.DECIMAL64)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new Quote(ticker, last.date(), last.close(), previous, changePct, history);
    }
}
