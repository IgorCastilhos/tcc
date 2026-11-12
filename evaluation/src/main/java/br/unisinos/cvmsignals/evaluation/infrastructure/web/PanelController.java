package br.unisinos.cvmsignals.evaluation.infrastructure.web;

import br.unisinos.cvmsignals.evaluation.application.port.PanelViewPort;
import br.unisinos.cvmsignals.evaluation.application.port.TickerMappingPort;
import br.unisinos.cvmsignals.evaluation.domain.TamQuestionnaire;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Alimenta a interface React do estudo. A resposta é autocontida — sinais,
 * cotações e o Quadro 4 no mesmo payload — para que o mesmo JSON sirva
 * tanto à aplicação rodando ao vivo quanto ao snapshot estático publicado
 * aos participantes.
 */
@RestController
@RequestMapping("/api/panel")
public class PanelController {

    /**
     * Repetido no payload para que o snapshot publicado carregue o aviso
     * junto com os dados, e não apenas no código da interface.
     */
    private static final String DISCLAIMER =
            "Conteúdo informativo gerado automaticamente a partir de documentos públicos da CVM. "
            + "Não constitui recomendação de investimento (Seção 4.5 da monografia).";

    private final PanelViewPort panel;
    private final TickerMappingPort tickers;

    public PanelController(PanelViewPort panel, TickerMappingPort tickers) {
        this.panel = panel;
        this.tickers = tickers;
    }

    @GetMapping
    public PanelResponse panel() {
        PanelViewPort.Panel loaded = panel.load();
        return new PanelResponse(loaded.generatedAt(), DISCLAIMER,
                TamQuestionnaire.statements(), loaded.companies());
    }

    /**
     * O catálogo de companhias da B3 conhecidas. Serve para escolher o que
     * ingerir: encadeie os CNPJs daqui no filtro de
     * {@code POST /api/ingestion/discover}.
     */
    @GetMapping("/tickers")
    public List<TickerMappingPort.TickerEntry> tickers() {
        return tickers.list();
    }

    /** Corrige o mapa CNPJ → B3 quando a semente da migration V2 estiver errada. */
    @PutMapping("/empresas/{cnpj}/ticker")
    public TickerResponse assignTicker(@PathVariable String cnpj, @RequestBody TickerRequest request) {
        if (request == null || request.ticker() == null || request.ticker().isBlank()) {
            throw new IllegalArgumentException("Informe o código de negociação (ex.: PETR4)");
        }
        tickers.assign(cnpj, request.ticker(), request.referenceName());
        return new TickerResponse(cnpj.replaceAll("\\D", ""), request.ticker().strip().toUpperCase(), "MANUAL");
    }

    public record PanelResponse(Instant generatedAt, String disclaimer,
                                List<String> statements,
                                List<PanelViewPort.CompanyCard> companies) {
    }

    public record TickerRequest(String ticker, String referenceName) {
    }

    public record TickerResponse(String cnpj, String ticker, String origin) {
    }
}
