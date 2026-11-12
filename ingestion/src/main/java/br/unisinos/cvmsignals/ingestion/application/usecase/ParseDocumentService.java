package br.unisinos.cvmsignals.ingestion.application.usecase;

import br.unisinos.cvmsignals.ingestion.application.port.DocumentDownloadPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentParserPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.IngestionEventPublisherPort;
import br.unisinos.cvmsignals.ingestion.domain.RegulatoryDocument;
import br.unisinos.cvmsignals.shared.events.DocumentParsed;

import java.util.UUID;

/**
 * Caso de uso: baixar o binário, extrair texto via Tika e publicar
 * cvm.docs.parsed. Falhas marcam o documento como FAILED em vez de
 * interromper o pipeline — documentos ruins são inspecionados depois.
 */
public class ParseDocumentService {

    private final DocumentRepositoryPort documents;
    private final DocumentDownloadPort downloader;
    private final DocumentParserPort parser;
    private final IngestionEventPublisherPort events;

    public ParseDocumentService(DocumentRepositoryPort documents, DocumentDownloadPort downloader,
                                DocumentParserPort parser, IngestionEventPublisherPort events) {
        this.documents = documents;
        this.downloader = downloader;
        this.parser = parser;
        this.events = events;
    }

    public void parse(UUID documentId, String cnpj) {
        RegulatoryDocument doc = documents.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Documento inexistente: " + documentId));
        try {
            byte[] content = downloader.download(doc.sourceUrl());
            doc.markParsed(parser.parse(content));
            documents.save(doc);
            events.publish(new DocumentParsed(doc.id(), cnpj, doc.type(), doc.text().length()));
        } catch (Exception e) {
            doc.markFailed();
            documents.save(doc);
            throw new DocumentParsingException(documentId, e);
        }
    }

    public static class DocumentParsingException extends RuntimeException {
        public DocumentParsingException(UUID documentId, Throwable cause) {
            super("Falha ao processar documento " + documentId, cause);
        }
    }
}
