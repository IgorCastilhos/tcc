package br.unisinos.cvmsignals.evaluation.application.usecase;

import br.unisinos.cvmsignals.evaluation.application.port.EvaluationRepositoryPort;
import br.unisinos.cvmsignals.evaluation.domain.UserEvaluation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Caso de uso: registrar a resposta semanal de um participante ao instrumento TAM. */
public class SubmitEvaluationService {

    private final EvaluationRepositoryPort evaluations;

    public SubmitEvaluationService(EvaluationRepositoryPort evaluations) {
        this.evaluations = evaluations;
    }

    public UUID submit(String participantCode, int week, Map<Integer, Integer> answers, String comments) {
        UserEvaluation evaluation = new UserEvaluation(
                UUID.randomUUID(), participantCode, week, answers, comments, Instant.now());
        evaluations.save(evaluation);
        return evaluation.id();
    }
}
