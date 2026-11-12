package br.unisinos.cvmsignals.evaluation.infrastructure.persistence;

import br.unisinos.cvmsignals.evaluation.application.port.SignalCatalogPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Consulta somente-leitura que junta sinal, documento e empresa para a interface de avaliação. */
@Component
public class JdbcSignalCatalogAdapter implements SignalCatalogPort {

    private static final String QUERY = """
            SELECT s.id, e.nome, e.cnpj, d.tipo, d.data_referencia,
                   s.modelo, s.tipo AS tipo_sinal, s.rotulo, s.confianca,
                   s.justificativa, s.evidencia, s.contextos
            FROM sinal s
            JOIN documento d ON d.id = s.documento_id
            JOIN empresa e ON e.id = d.empresa_id
            ORDER BY e.nome, d.data_referencia DESC, s.modelo, s.tipo
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcSignalCatalogAdapter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SignalView> listSignals() {
        return jdbc.query(QUERY, (rs, i) -> new SignalView(
                rs.getObject("id", UUID.class),
                rs.getString("nome"),
                rs.getString("cnpj"),
                rs.getString("tipo"),
                rs.getObject("data_referencia", LocalDate.class),
                rs.getString("modelo"),
                rs.getString("tipo_sinal"),
                rs.getString("rotulo"),
                rs.getDouble("confianca"),
                rs.getString("justificativa"),
                rs.getString("evidencia"),
                parseContexts(rs.getString("contextos"))));
    }

    private List<String> parseContexts(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
