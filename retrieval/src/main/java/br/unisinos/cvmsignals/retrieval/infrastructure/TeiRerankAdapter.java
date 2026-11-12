package br.unisinos.cvmsignals.retrieval.infrastructure;

import br.unisinos.cvmsignals.retrieval.application.port.RerankPort;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Cliente do endpoint /rerank do HuggingFace Text Embeddings Inference
 * servindo BAAI/bge-reranker-v2-m3. O TEI é usado porque o Ollama não
 * expõe API de reranking cross-encoder (ver ADR-0003).
 */
@Component
public class TeiRerankAdapter implements RerankPort {

    private final RestClient teiRestClient;

    public TeiRerankAdapter(@Qualifier("teiRestClient") RestClient teiRestClient) {
        this.teiRestClient = teiRestClient;
    }

    @Override
    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK) {
        List<String> texts = candidates.stream().map(ScoredChunk::content).toList();
        List<RerankResult> results = teiRestClient.post()
                .uri("/rerank")
                .body(Map.of("query", query, "texts", texts))
                .retrieve()
                .body(new ParameterizedTypeReference<List<RerankResult>>() {
                });
        if (results == null) {
            throw new IllegalStateException("Resposta vazia do reranker TEI");
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(RerankResult::score).reversed())
                .limit(topK)
                .map(r -> candidates.get(r.index()).withScore(r.score()))
                .toList();
    }

    record RerankResult(int index, double score) {
    }
}
