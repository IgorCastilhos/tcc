package br.unisinos.cvmsignals.retrieval.application.port;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;

import java.util.List;

/** Busca por similaridade de cosseno sobre os embeddings BGE-M3 no pgvector. */
public interface DenseSearchPort {

    List<ScoredChunk> search(RetrievalRequest request, float[] queryEmbedding, int limit);
}
