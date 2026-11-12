package br.unisinos.cvmsignals.retrieval.application.port;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;

import java.util.List;

/** Busca lexical (full-text search em português) — o braço esparso da recuperação híbrida. */
public interface SparseSearchPort {

    List<ScoredChunk> search(RetrievalRequest request, int limit);
}
