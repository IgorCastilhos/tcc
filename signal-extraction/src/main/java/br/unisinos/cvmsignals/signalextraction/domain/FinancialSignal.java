package br.unisinos.cvmsignals.signalextraction.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado central do TCC: um sinal financeiro estruturado extraído de um
 * documento por um LLM. Guarda a justificativa, a evidência textual e os
 * contextos recuperados — insumos da avaliação RAGAS (faithfulness) e da
 * auditoria humana.
 */
public final class FinancialSignal {

    private final UUID id;
    private final UUID documentId;
    private final LlmProvider model;
    private final SignalType type;
    private final String label;
    private final double confidence;
    private final String justification;
    private final String evidence;
    private final List<String> contexts;
    private final Instant createdAt;

    public FinancialSignal(UUID id, UUID documentId, LlmProvider model, SignalType type,
                           String label, double confidence, String justification,
                           String evidence, List<String> contexts, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.documentId = Objects.requireNonNull(documentId);
        this.model = Objects.requireNonNull(model);
        this.type = Objects.requireNonNull(type);
        this.label = requireLabel(label);
        this.confidence = requireConfidence(confidence);
        this.justification = justification;
        this.evidence = evidence;
        this.contexts = List.copyOf(contexts == null ? List.of() : contexts);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    private static String requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Sinal sem rótulo");
        }
        return label.strip().toUpperCase();
    }

    private static double requireConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confiança fora de [0,1]: " + confidence);
        }
        return confidence;
    }

    public UUID id() { return id; }
    public UUID documentId() { return documentId; }
    public LlmProvider model() { return model; }
    public SignalType type() { return type; }
    public String label() { return label; }
    public double confidence() { return confidence; }
    public String justification() { return justification; }
    public String evidence() { return evidence; }
    public List<String> contexts() { return contexts; }
    public Instant createdAt() { return createdAt; }
}
