package br.unisinos.cvmsignals.indexing.application.port;

import br.unisinos.cvmsignals.indexing.domain.Chunk;

import java.util.List;
import java.util.UUID;

public interface ChunkRepositoryPort {

    void saveAll(List<Chunk> chunks);

    void deleteByDocumentId(UUID documentId);
}
