package br.unisinos.cvmsignals.indexing.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Trecho indexável de um documento. O embedding (vector(1024), BGE-M3)
 * e os metadados JSONB — cnpj, tipo, dataReferencia — habilitam a busca
 * híbrida com filtros combinados descrita na Seção 4.3.
 */
public record Chunk(
        UUID id,
        UUID documentId,
        int seq,
        String content,
        float[] embedding,
        Map<String, String> metadata) {

    public Chunk {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(metadata, "metadata");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Chunk sem conteúdo");
        }
        if (seq < 0) {
            throw new IllegalArgumentException("seq negativo");
        }
    }

    public Chunk withEmbedding(float[] vector) {
        return new Chunk(id, documentId, seq, content, vector, metadata);
    }
}
