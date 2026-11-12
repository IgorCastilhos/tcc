package br.unisinos.cvmsignals.signalextraction.infrastructure.persistence;

import br.unisinos.cvmsignals.signalextraction.domain.LlmProvider;
import br.unisinos.cvmsignals.signalextraction.domain.SignalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sinal")
public class SignalEntity {

    @Id
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmProvider modelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalType tipo;

    @Column(nullable = false)
    private String rotulo;

    @Column(nullable = false)
    private double confianca;

    @Column(columnDefinition = "text")
    private String justificativa;

    @Column(columnDefinition = "text")
    private String evidencia;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> contextos;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected SignalEntity() {
    }

    public SignalEntity(UUID id, UUID documentoId, LlmProvider modelo, SignalType tipo, String rotulo,
                        double confianca, String justificativa, String evidencia,
                        List<String> contextos, Instant criadoEm) {
        this.id = id;
        this.documentoId = documentoId;
        this.modelo = modelo;
        this.tipo = tipo;
        this.rotulo = rotulo;
        this.confianca = confianca;
        this.justificativa = justificativa;
        this.evidencia = evidencia;
        this.contextos = contextos;
        this.criadoEm = criadoEm;
    }

    public UUID getId() { return id; }
    public UUID getDocumentoId() { return documentoId; }
    public LlmProvider getModelo() { return modelo; }
    public SignalType getTipo() { return tipo; }
    public String getRotulo() { return rotulo; }
    public double getConfianca() { return confianca; }
    public String getJustificativa() { return justificativa; }
    public String getEvidencia() { return evidencia; }
    public List<String> getContextos() { return contextos; }
    public Instant getCriadoEm() { return criadoEm; }
}
