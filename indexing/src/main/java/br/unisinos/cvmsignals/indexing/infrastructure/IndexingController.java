package br.unisinos.cvmsignals.indexing.infrastructure;

import br.unisinos.cvmsignals.indexing.application.usecase.IndexDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reindexação manual. Falhas transitórias (Ollama indisponível, timeout)
 * deixam documentos PARSED sem chunks, e o evento Kafka correspondente já
 * foi consumido — este endpoint é o caminho de recuperação.
 */
@RestController
@RequestMapping("/api/indexing")
public class IndexingController {

    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    private static final String PENDING = """
            SELECT d.id FROM documento d
            LEFT JOIN chunk c ON c.documento_id = d.id
            WHERE d.status = 'PARSED'
            GROUP BY d.id HAVING count(c.id) = 0
            """;

    private final IndexDocumentService indexDocument;
    private final JdbcTemplate jdbc;

    public IndexingController(IndexDocumentService indexDocument, JdbcTemplate jdbc) {
        this.indexDocument = indexDocument;
        this.jdbc = jdbc;
    }

    @PostMapping("/reindex/{documentId}")
    public Map<String, Object> reindex(@PathVariable UUID documentId) {
        return Map.of("documentId", documentId, "chunks", indexDocument.index(documentId));
    }

    /** Indexa todos os documentos parseados que ainda não têm chunks. */
    @PostMapping("/reindex-pending")
    public Map<String, Object> reindexPending() {
        List<UUID> pending = jdbc.queryForList(PENDING, UUID.class);
        Map<String, Object> result = new LinkedHashMap<>();
        int ok = 0;
        for (UUID id : pending) {
            try {
                result.put(id.toString(), indexDocument.index(id));
                ok++;
            } catch (RuntimeException e) {
                // Um documento problemático não interrompe o lote.
                log.error("Falha ao indexar {}: {}", id, e.getMessage());
                result.put(id.toString(), "ERRO: " + e.getMessage());
            }
        }
        return Map.of("pendentes", pending.size(), "indexados", ok, "detalhe", result);
    }
}