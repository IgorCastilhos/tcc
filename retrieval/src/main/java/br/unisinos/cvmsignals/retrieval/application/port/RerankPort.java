package br.unisinos.cvmsignals.retrieval.application.port;

import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;

import java.util.List;

/** Reordenação cross-encoder (BGE-reranker-v2-m3) dos candidatos fundidos. */
public interface RerankPort {

    List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK);
}
