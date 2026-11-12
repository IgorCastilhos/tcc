package br.unisinos.cvmsignals.signalextraction.infrastructure;

import br.unisinos.cvmsignals.retrieval.application.RetrievalRequest;
import br.unisinos.cvmsignals.retrieval.application.usecase.HybridRetrievalService;
import br.unisinos.cvmsignals.retrieval.domain.ScoredChunk;
import br.unisinos.cvmsignals.signalextraction.application.port.DocumentContextPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de contexto: delega a recuperação ao contexto Retrieval
 * restringindo a busca ao documento-alvo, de modo que cada sinal seja
 * sustentado apenas pelo próprio documento que o originou.
 */
@Component
public class RetrievalDocumentContextAdapter implements DocumentContextPort {

    private final HybridRetrievalService retrieval;
    private final JdbcTemplate jdbc;

    public RetrievalDocumentContextAdapter(HybridRetrievalService retrieval, JdbcTemplate jdbc) {
        this.retrieval = retrieval;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<DocumentInfo> documentInfo(UUID documentId) {
        return jdbc.query("""
                        SELECT d.id, e.cnpj, e.nome, d.tipo
                        FROM documento d JOIN empresa e ON e.id = d.empresa_id
                        WHERE d.id = ?
                        """,
                rs -> rs.next()
                        ? Optional.of(new DocumentInfo(rs.getObject("id", UUID.class),
                        rs.getString("cnpj"), rs.getString("nome"), rs.getString("tipo")))
                        : Optional.empty(), documentId);
    }

    @Override
    public List<String> topChunks(UUID documentId, String query, int topK) {
        return retrieval.retrieve(RetrievalRequest.of(query, topK).forDocument(documentId))
                .stream()
                .map(ScoredChunk::content)
                .toList();
    }
}
