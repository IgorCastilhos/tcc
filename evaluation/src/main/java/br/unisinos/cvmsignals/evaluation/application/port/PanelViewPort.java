package br.unisinos.cvmsignals.evaluation.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read model do painel apresentado aos participantes do estudo: uma linha
 * por companhia, com a cotação corrente e a leitura de cada um dos três
 * LLMs sobre o mesmo documento.
 *
 * <p>O pipeline não produz recomendação de investimento (Seção 4.5 da
 * monografia); cada modelo devolve sentimento, risco e eventos, e é isso
 * que o painel exibe lado a lado.
 */
public interface PanelViewPort {

    Panel load();

    record Panel(Instant generatedAt, List<CompanyCard> companies) {
    }

    /** Uma companhia com seus documentos analisados e, quando houver, a cotação. */
    record CompanyCard(
            String cnpj,
            String name,
            String ticker,
            String tickerOrigin,
            Quote quote,
            List<DocumentCard> documents) {
    }

    /**
     * Cotação mais recente em serie_preco. {@code changePct} compara o
     * último fechamento com o anterior; é nulo quando só há um ponto.
     */
    record Quote(
            String ticker,
            LocalDate date,
            BigDecimal close,
            BigDecimal previousClose,
            Double changePct,
            List<PricePoint> history) {
    }

    record PricePoint(LocalDate date, BigDecimal close) {
    }

    /**
     * {@code sourceUrl} e {@code protocol} são a rastreabilidade do sinal:
     * permitem ao participante sair do trecho citado pelo modelo e chegar ao
     * documento original publicado na CVM.
     */
    record DocumentCard(
            UUID documentId,
            String type,
            LocalDate referenceDate,
            String sourceUrl,
            String protocol,
            List<ModelReading> readings) {
    }

    /** A leitura de um LLM sobre um documento: os três tipos de sinal juntos. */
    record ModelReading(
            String model,
            Verdict sentiment,
            Verdict risk,
            List<Verdict> events) {
    }

    record Verdict(String label, double confidence, String justification, String evidence) {
    }
}
