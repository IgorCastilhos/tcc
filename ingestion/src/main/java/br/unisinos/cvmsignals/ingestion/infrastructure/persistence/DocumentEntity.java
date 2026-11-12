package br.unisinos.cvmsignals.ingestion.infrastructure.persistence;

import br.unisinos.cvmsignals.ingestion.domain.DocumentStatus;
import br.unisinos.cvmsignals.shared.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "documento")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType tipo;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "url_origem", nullable = false, unique = true, columnDefinition = "text")
    private String urlOrigem;

    private String protocolo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(columnDefinition = "text")
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected DocumentEntity() {
    }

    public DocumentEntity(UUID id, UUID empresaId, DocumentType tipo, LocalDate dataReferencia,
                          String urlOrigem, String protocolo, DocumentStatus status, String texto) {
        this.id = id;
        this.empresaId = empresaId;
        this.tipo = tipo;
        this.dataReferencia = dataReferencia;
        this.urlOrigem = urlOrigem;
        this.protocolo = protocolo;
        this.status = status;
        this.texto = texto;
    }

    public UUID getId() { return id; }
    public UUID getEmpresaId() { return empresaId; }
    public DocumentType getTipo() { return tipo; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public String getUrlOrigem() { return urlOrigem; }
    public String getProtocolo() { return protocolo; }
    public DocumentStatus getStatus() { return status; }
    public String getTexto() { return texto; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public void setTexto(String texto) { this.texto = texto; }
}
