package br.unisinos.cvmsignals.evaluation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "avaliacao_usuario")
public class EvaluationEntity {

    @Id
    private UUID id;

    @Column(name = "participante", nullable = false)
    private String participante;

    @Column(nullable = false)
    private int semana;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<Integer, Integer> respostas;

    @Column(columnDefinition = "text")
    private String comentario;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected EvaluationEntity() {
    }

    public EvaluationEntity(UUID id, String participante, int semana,
                            Map<Integer, Integer> respostas, String comentario, Instant criadoEm) {
        this.id = id;
        this.participante = participante;
        this.semana = semana;
        this.respostas = respostas;
        this.comentario = comentario;
        this.criadoEm = criadoEm;
    }

    public UUID getId() { return id; }
    public String getParticipante() { return participante; }
    public int getSemana() { return semana; }
    public Map<Integer, Integer> getRespostas() { return respostas; }
    public String getComentario() { return comentario; }
    public Instant getCriadoEm() { return criadoEm; }
}
