package br.unisinos.cvmsignals.retrieval.infrastructure;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.application.port.DenseSearchPort;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Similaridade de cosseno via operador <=> do pgvector, acelerada pelo índice HNSW. */
@Component
public class PgDenseSearchAdapter implements DenseSearchPort {

    private final JdbcTemplate jdbc;

    public PgDenseSearchAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ScoredChunk> search(RetrievalRequest request, float[] queryEmbedding, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, documento_id, conteudo, 1 - (embedding <=> ?::vector) AS score
                FROM chunk WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        params.add(toVectorLiteral(queryEmbedding));
        appendFilters(request, sql, params);
        sql.append(" ORDER BY embedding <=> ?::vector LIMIT ?");
        params.add(toVectorLiteral(queryEmbedding));
        params.add(limit);

        return jdbc.query(sql.toString(), (rs, i) -> new ScoredChunk(
                rs.getObject("id", UUID.class),
                rs.getObject("documento_id", UUID.class),
                rs.getString("conteudo"),
                rs.getDouble("score")), params.toArray());
    }

    static void appendFilters(RetrievalRequest request, StringBuilder sql, List<Object> params) {
        request.cnpj().ifPresent(cnpj -> {
            sql.append(" AND metadata->>'cnpj' = ?");
            params.add(cnpj);
        });
        request.documentType().ifPresent(type -> {
            sql.append(" AND metadata->>'tipo' = ?");
            params.add(type);
        });
        request.documentId().ifPresent(docId -> {
            sql.append(" AND documento_id = ?");
            params.add(docId);
        });
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
