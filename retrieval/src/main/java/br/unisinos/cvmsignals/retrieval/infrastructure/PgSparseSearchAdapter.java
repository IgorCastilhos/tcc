package br.unisinos.cvmsignals.retrieval.infrastructure;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.application.port.SparseSearchPort;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Braço esparso: full-text search do PostgreSQL com dicionário 'portuguese'. */
@Component
public class PgSparseSearchAdapter implements SparseSearchPort {

    private final JdbcTemplate jdbc;

    public PgSparseSearchAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ScoredChunk> search(RetrievalRequest request, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, documento_id, conteudo,
                       ts_rank_cd(content_tsv, websearch_to_tsquery('portuguese', ?)) AS score
                FROM chunk
                WHERE content_tsv @@ websearch_to_tsquery('portuguese', ?)
                """);
        List<Object> params = new ArrayList<>();
        params.add(request.query());
        params.add(request.query());
        PgDenseSearchAdapter.appendFilters(request, sql, params);
        sql.append(" ORDER BY score DESC LIMIT ?");
        params.add(limit);

        return jdbc.query(sql.toString(), (rs, i) -> new ScoredChunk(
                rs.getObject("id", UUID.class),
                rs.getObject("documento_id", UUID.class),
                rs.getString("conteudo"),
                rs.getDouble("score")), params.toArray());
    }
}
