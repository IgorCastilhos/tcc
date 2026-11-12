package br.unisinos.cvmsignals.retrieval.application.usecase;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.application.port.DenseSearchPort;
import br.unisinos.cvmsignals.retrieval.application.port.QueryEmbeddingPort;
import br.unisinos.cvmsignals.retrieval.application.port.RerankPort;
import br.unisinos.cvmsignals.retrieval.application.port.SparseSearchPort;
import br.unisinos.cvmsignals.retrieval.domain.ReciprocalRankFusion;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;

import java.util.List;

/**
 * Estratégia recomendada para o BGE-M3 (Chen et al., 2024): recuperação
 * híbrida (dense + sparse), fusão RRF e reranking cross-encoder no topo.
 */
public class HybridRetrievalService {

    private final QueryEmbeddingPort queryEmbedding;
    private final DenseSearchPort denseSearch;
    private final SparseSearchPort sparseSearch;
    private final RerankPort reranker;
    private final ReciprocalRankFusion fusion;
    private final int candidatesPerArm;

    public HybridRetrievalService(QueryEmbeddingPort queryEmbedding, DenseSearchPort denseSearch,
                                  SparseSearchPort sparseSearch, RerankPort reranker,
                                  int rrfK, int candidatesPerArm) {
        this.queryEmbedding = queryEmbedding;
        this.denseSearch = denseSearch;
        this.sparseSearch = sparseSearch;
        this.reranker = reranker;
        this.fusion = new ReciprocalRankFusion(rrfK);
        this.candidatesPerArm = candidatesPerArm;
    }

    public List<ScoredChunk> retrieve(RetrievalRequest request) {
        float[] embedding = queryEmbedding.embed(request.query());
        List<ScoredChunk> dense = denseSearch.search(request, embedding, candidatesPerArm);
        List<ScoredChunk> sparse = sparseSearch.search(request, candidatesPerArm);
        List<ScoredChunk> fused = fusion.fuse(dense, sparse);
        if (fused.isEmpty()) {
            return fused;
        }
        return reranker.rerank(request.query(), fused, request.topK());
    }
}
