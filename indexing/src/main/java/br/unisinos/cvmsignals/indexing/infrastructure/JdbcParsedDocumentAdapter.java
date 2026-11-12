package br.unisinos.cvmsignals.indexing.infrastructure;

import br.unisinos.cvmsignals.indexing.application.port.ParsedDocumentPort;
import br.unisinos.cvmsignals.shared.DocumentType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcParsedDocumentAdapter implements ParsedDocumentPort {

    private static final String QUERY = """
            SELECT d.id, e.cnpj, d.tipo, d.data_referencia, d.texto
            FROM documento d JOIN empresa e ON e.id = d.empresa_id
            WHERE d.id = ? AND d.status = 'PARSED'
            """;

    private final JdbcTemplate jdbc;

    public JdbcParsedDocumentAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ParsedDocument> findById(UUID documentId) {
        return jdbc.query(QUERY, rs -> rs.next()
                ? Optional.of(new ParsedDocument(
                        rs.getObject("id", UUID.class),
                        rs.getString("cnpj"),
                        DocumentType.valueOf(rs.getString("tipo")),
                        rs.getObject("data_referencia", LocalDate.class),
                        rs.getString("texto")))
                : Optional.empty(), documentId);
    }
}
