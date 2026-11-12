package br.unisinos.cvmsignals.ingestion.infrastructure.web;

import br.unisinos.cvmsignals.ingestion.application.usecase.DiscoverDocumentsService;
import br.unisinos.cvmsignals.ingestion.application.usecase.RetryFailedDocumentsService;
import br.unisinos.cvmsignals.shared.DocumentType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final DiscoverDocumentsService discoverDocuments;
    private final RetryFailedDocumentsService retryFailed;

    public IngestionController(DiscoverDocumentsService discoverDocuments,
                               RetryFailedDocumentsService retryFailed) {
        this.discoverDocuments = discoverDocuments;
        this.retryFailed = retryFailed;
    }

    /**
     * Ex.: POST /api/ingestion/discover?type=FATO_RELEVANTE&year=2026&limit=50
     *
     * <p>Para cobrir várias companhias em vez de esgotar o limite nas
     * primeiras da lista da CVM, restrinja por CNPJ e limite o número de
     * documentos por empresa:
     * {@code ...&cnpj=33000167000101&cnpj=33592510000154&maxPorEmpresa=1}
     */
    @PostMapping("/discover")
    public Map<String, Object> discover(@RequestParam DocumentType type,
                                        @RequestParam int year,
                                        @RequestParam(defaultValue = "50") int limit,
                                        @RequestParam(required = false) List<String> cnpj,
                                        @RequestParam(required = false) Integer maxPorEmpresa) {
        Set<String> filtro = cnpj == null ? Set.of() : Set.copyOf(cnpj);
        int teto = maxPorEmpresa == null ? Integer.MAX_VALUE : maxPorEmpresa;
        int published = discoverDocuments.discover(type, year, limit, filtro, teto);
        return Map.of("type", type, "year", year,
                "empresasFiltradas", filtro.size(),
                "maxPorEmpresa", maxPorEmpresa == null ? "sem teto" : maxPorEmpresa,
                "publicados", published);
    }

    /**
     * Nova tentativa de download/parse dos documentos marcados FAILED.
     *
     * <p>Roda em série e com pausa: a falha típica é o ENET recusar uma
     * rajada, então repetir devagar recupera boa parte do lote.
     */
    @PostMapping("/reparse-failed")
    public RetryFailedDocumentsService.Result reparseFailed(
            @RequestParam(defaultValue = "50") int limit) {
        return retryFailed.retry(limit);
    }
}
