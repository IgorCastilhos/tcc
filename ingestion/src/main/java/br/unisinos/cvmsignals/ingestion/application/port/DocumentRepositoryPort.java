package br.unisinos.cvmsignals.ingestion.application.port;

import br.unisinos.cvmsignals.ingestion.domain.RegulatoryDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {

    void save(RegulatoryDocument document);

    Optional<RegulatoryDocument> findById(UUID id);

    boolean existsBySourceUrl(String sourceUrl);

    /** Documentos marcados FAILED, para uma nova tentativa de download/parse. */
    List<FailedDocument> findFailed(int limit);

    record FailedDocument(UUID id, String cnpj) {
    }
}
