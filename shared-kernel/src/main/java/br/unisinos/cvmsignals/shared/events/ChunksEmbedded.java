package br.unisinos.cvmsignals.shared.events;

import java.util.UUID;

/** Publicado em cvm.chunks.embedded quando os chunks de um documento foram vetorizados e indexados. */
public record ChunksEmbedded(UUID documentId, String cnpj, int chunkCount) {
}
