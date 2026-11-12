package br.unisinos.cvmsignals.indexing.application.usecase;

import br.unisinos.cvmsignals.indexing.application.port.ChunkRepositoryPort;
import br.unisinos.cvmsignals.indexing.application.port.EmbeddingPort;
import br.unisinos.cvmsignals.indexing.application.port.IndexingEventPublisherPort;
import br.unisinos.cvmsignals.indexing.application.port.ParsedDocumentPort;
import br.unisinos.cvmsignals.indexing.domain.Chunk;
import br.unisinos.cvmsignals.indexing.domain.ChunkingPolicy;
import br.unisinos.cvmsignals.shared.events.ChunksEmbedded;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso: segmentar o texto, gerar embeddings BGE-M3 em lotes e
 * persistir no pgvector. Reindexações substituem os chunks anteriores,
 * mantendo o índice consistente com a versão mais recente do texto.
 */
public class IndexDocumentService {

    private static final int EMBEDDING_BATCH_SIZE = 16;

    private final ParsedDocumentPort parsedDocuments;
    private final ChunkRepositoryPort chunks;
    private final EmbeddingPort embeddings;
    private final IndexingEventPublisherPort events;
    private final ChunkingPolicy chunkingPolicy;

    public IndexDocumentService(ParsedDocumentPort parsedDocuments, ChunkRepositoryPort chunks,
                                EmbeddingPort embeddings, IndexingEventPublisherPort events,
                                ChunkingPolicy chunkingPolicy) {
        this.parsedDocuments = parsedDocuments;
        this.chunks = chunks;
        this.embeddings = embeddings;
        this.events = events;
        this.chunkingPolicy = chunkingPolicy;
    }

    public int index(UUID documentId) {
        ParsedDocumentPort.ParsedDocument doc = parsedDocuments.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não parseado: " + documentId));

        List<String> pieces = chunkingPolicy.split(doc.text());
        Map<String, String> metadata = Map.of(
                "cnpj", doc.cnpj(),
                "tipo", doc.type().name(),
                "dataReferencia", doc.referenceDate().toString());

        List<Chunk> embedded = new ArrayList<>(pieces.size());
        for (int start = 0; start < pieces.size(); start += EMBEDDING_BATCH_SIZE) {
            List<String> batch = pieces.subList(start, Math.min(start + EMBEDDING_BATCH_SIZE, pieces.size()));
            List<float[]> vectors = embeddings.embed(batch);
            for (int i = 0; i < batch.size(); i++) {
                embedded.add(new Chunk(UUID.randomUUID(), documentId, start + i, batch.get(i),
                        vectors.get(i), metadata));
            }
        }

        chunks.deleteByDocumentId(documentId);
        chunks.saveAll(embedded);
        events.publish(new ChunksEmbedded(documentId, doc.cnpj(), embedded.size()));
        return embedded.size();
    }
}
