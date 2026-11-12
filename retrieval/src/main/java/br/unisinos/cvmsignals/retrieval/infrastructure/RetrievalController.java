package br.unisinos.cvmsignals.retrieval.infrastructure;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.application.usecase.HybridRetrievalService;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/retrieval")
public class RetrievalController {

    private final HybridRetrievalService retrieval;

    public RetrievalController(HybridRetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @GetMapping("/search")
    public List<ScoredChunk> search(@RequestParam String q,
                                    @RequestParam(required = false) String cnpj,
                                    @RequestParam(required = false) String tipo,
                                    @RequestParam(defaultValue = "5") int topK) {
        return retrieval.retrieve(new RetrievalRequest(
                q, Optional.ofNullable(cnpj), Optional.ofNullable(tipo), Optional.empty(), topK));
    }
}
