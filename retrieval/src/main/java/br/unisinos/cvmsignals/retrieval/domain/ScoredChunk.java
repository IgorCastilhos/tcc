package br.unisinos.cvmsignals.retrieval.domain;

import java.util.UUID;

/** Chunk recuperado com sua pontuação na etapa corrente (dense, sparse, RRF ou rerank). */
public record ScoredChunk(UUID chunkId, UUID documentId, String content, double score) {

    public ScoredChunk withScore(double newScore) {
        return new ScoredChunk(chunkId, documentId, content, newScore);
    }
}
