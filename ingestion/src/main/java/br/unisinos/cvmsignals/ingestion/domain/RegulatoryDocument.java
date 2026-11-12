package br.unisinos.cvmsignals.ingestion.domain;

import br.unisinos.cvmsignals.shared.DocumentType;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado central do contexto Ingestion: um documento regulatório
 * (DFP, ITR ou Fato Relevante) coletado da CVM. O texto extraído é
 * mantido no agregado porque é a matéria-prima dos contextos seguintes.
 */
public final class RegulatoryDocument {

    private final UUID id;
    private final UUID companyId;
    private final DocumentType type;
    private final LocalDate referenceDate;
    private final String sourceUrl;
    private final String protocol;
    private DocumentStatus status;
    private String text;

    public RegulatoryDocument(UUID id, UUID companyId, DocumentType type,
                              LocalDate referenceDate, String sourceUrl, String protocol) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.type = Objects.requireNonNull(type, "type");
        this.referenceDate = Objects.requireNonNull(referenceDate, "referenceDate");
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl");
        this.protocol = protocol;
        this.status = DocumentStatus.DISCOVERED;
    }

    /** Registra o texto extraído; textos muito curtos indicam falha de parsing ou OCR. */
    public void markParsed(String extractedText) {
        Objects.requireNonNull(extractedText, "extractedText");
        String normalized = extractedText.strip();
        if (normalized.length() < 100) {
            throw new IllegalArgumentException(
                    "Texto extraído curto demais (" + normalized.length() + " chars); provável falha de parsing");
        }
        this.text = normalized;
        this.status = DocumentStatus.PARSED;
    }

    public void markFailed() {
        this.status = DocumentStatus.FAILED;
    }

    public UUID id() { return id; }
    public UUID companyId() { return companyId; }
    public DocumentType type() { return type; }
    public LocalDate referenceDate() { return referenceDate; }
    public String sourceUrl() { return sourceUrl; }
    public String protocol() { return protocol; }
    public DocumentStatus status() { return status; }
    public String text() { return text; }

    /** Reconstrução a partir da persistência, sem passar pelas invariantes de criação. */
    public static RegulatoryDocument restore(UUID id, UUID companyId, DocumentType type, LocalDate referenceDate,
                                             String sourceUrl, String protocol, DocumentStatus status, String text) {
        RegulatoryDocument doc = new RegulatoryDocument(id, companyId, type, referenceDate, sourceUrl, protocol);
        doc.status = status;
        doc.text = text;
        return doc;
    }
}
