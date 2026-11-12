package br.unisinos.cvmsignals.evaluation.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Resposta de um participante ao instrumento TAM em uma semana do estudo.
 * O participante é identificado por código pseudonimizado, em conformidade
 * com o TCLE e a Resolução CNS 510/2016 (Seção 4.5).
 */
public final class UserEvaluation {

    private final UUID id;
    private final String participantCode;
    private final int week;
    private final Map<Integer, Integer> answers;
    private final String comments;
    private final Instant createdAt;

    public UserEvaluation(UUID id, String participantCode, int week,
                          Map<Integer, Integer> answers, String comments, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.participantCode = requireParticipant(participantCode);
        this.week = requireWeek(week);
        TamQuestionnaire.validateAnswers(answers);
        this.answers = Map.copyOf(answers);
        this.comments = comments;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    private static String requireParticipant(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Código do participante é obrigatório");
        }
        return code.strip();
    }

    private static int requireWeek(int week) {
        if (week < 1 || week > 3) {
            throw new IllegalArgumentException("O estudo prevê 3 semanas; semana inválida: " + week);
        }
        return week;
    }

    public double averageFor(TamQuestionnaire.Dimension dimension) {
        return answers.entrySet().stream()
                .filter(e -> TamQuestionnaire.dimensionOf(e.getKey()) == dimension)
                .mapToInt(Map.Entry::getValue)
                .average()
                .orElse(0.0);
    }

    public UUID id() { return id; }
    public String participantCode() { return participantCode; }
    public int week() { return week; }
    public Map<Integer, Integer> answers() { return answers; }
    public String comments() { return comments; }
    public Instant createdAt() { return createdAt; }
}
