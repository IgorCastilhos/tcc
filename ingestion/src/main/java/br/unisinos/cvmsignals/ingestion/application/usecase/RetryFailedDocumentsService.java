package br.unisinos.cvmsignals.ingestion.application.usecase;

import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort.FailedDocument;

import java.util.List;

/**
 * Caso de uso: reprocessar documentos marcados FAILED.
 *
 * <p>A causa mais comum de falha é o ENET recusar uma rajada de downloads e
 * devolver a página intermediária; a mesma URL costuma funcionar depois.
 * Reprocessar em série, com pausa entre os documentos, recupera a maior
 * parte do lote sem precisar redescobrir nada.
 */
public class RetryFailedDocumentsService {

    /** Pausa entre documentos, para não reproduzir a rajada que causou a falha. */
    private final long pauseMillis;
    private final DocumentRepositoryPort documents;
    private final ParseDocumentService parseDocument;

    public RetryFailedDocumentsService(DocumentRepositoryPort documents,
                                       ParseDocumentService parseDocument,
                                       long pauseMillis) {
        this.documents = documents;
        this.parseDocument = parseDocument;
        this.pauseMillis = Math.max(0, pauseMillis);
    }

    public Result retry(int limit) {
        List<FailedDocument> failed = documents.findFailed(limit);
        int recovered = 0;
        int stillFailing = 0;
        for (FailedDocument doc : failed) {
            try {
                parseDocument.parse(doc.id(), doc.cnpj());
                recovered++;
            } catch (RuntimeException e) {
                stillFailing++;
            }
            pause();
        }
        return new Result(failed.size(), recovered, stillFailing);
    }

    private void pause() {
        if (pauseMillis == 0) {
            return;
        }
        try {
            Thread.sleep(pauseMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reprocessamento interrompido", e);
        }
    }

    public record Result(int tentados, int recuperados, int aindaFalhando) {
    }
}
