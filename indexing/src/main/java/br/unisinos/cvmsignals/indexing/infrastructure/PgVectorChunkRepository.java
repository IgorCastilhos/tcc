package br.unisinos.cvmsignals.indexing.infrastructure;

import br.unisinos.cvmsignals.indexing.application.port.ChunkRepositoryPort;
import br.unisinos.cvmsignals.indexing.domain.Chunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Persistência direta no esquema definido pela monografia (Figura 2):
 * coluna vector(1024) + metadata JSONB. JDBC puro dá controle total
 * sobre casts ::vector e ::jsonb, dispensando tabela genérica de vector store.
 */
@Repository
public class PgVectorChunkRepository implements ChunkRepositoryPort {

    private static final String INSERT = """
            INSERT INTO chunk (id, documento_id, seq, conteudo, embedding, metadata)
            VALUES (?, ?, ?, ?, ?::vector, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PgVectorChunkRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveAll(List<Chunk> chunks) {
        jdbc.batchUpdate(INSERT, chunks, chunks.size(), (ps, chunk) -> {
            ps.setObject(1, chunk.id());
            ps.setObject(2, chunk.documentId());
            ps.setInt(3, chunk.seq());
            ps.setString(4, chunk.content());
            ps.setString(5, toVectorLiteral(chunk.embedding()));
            ps.setString(6, toJson(chunk.metadata()));
        });
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        jdbc.update("DELETE FROM chunk WHERE documento_id = ?", documentId);
    }

    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar metadata", e);
        }
    }
}
