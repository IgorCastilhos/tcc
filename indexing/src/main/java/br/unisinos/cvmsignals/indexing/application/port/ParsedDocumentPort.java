package br.unisinos.cvmsignals.indexing.application.port;

import br.unisinos.cvmsignals.shared.DocumentType;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Acesso somente-leitura ao texto persistido pelo contexto Ingestion. */
public interface ParsedDocumentPort {

    Optional<ParsedDocument> findById(UUID documentId);

    record ParsedDocument(UUID id, String cnpj, DocumentType type, LocalDate referenceDate, String text) {
    }
}
