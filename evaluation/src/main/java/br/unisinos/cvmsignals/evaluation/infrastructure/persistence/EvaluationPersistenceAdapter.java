package br.unisinos.cvmsignals.evaluation.infrastructure.persistence;

import br.unisinos.cvmsignals.evaluation.application.port.EvaluationRepositoryPort;
import br.unisinos.cvmsignals.evaluation.domain.UserEvaluation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class EvaluationPersistenceAdapter implements EvaluationRepositoryPort {

    private final SpringDataEvaluations evaluations;

    public EvaluationPersistenceAdapter(SpringDataEvaluations evaluations) {
        this.evaluations = evaluations;
    }

    @Override
    public void save(UserEvaluation evaluation) {
        evaluations.save(new EvaluationEntity(evaluation.id(), evaluation.participantCode(),
                evaluation.week(), evaluation.answers(), evaluation.comments(), evaluation.createdAt()));
    }

    @Override
    public List<UserEvaluation> findAll() {
        return evaluations.findAll().stream()
                .map(e -> new UserEvaluation(e.getId(), e.getParticipante(), e.getSemana(),
                        e.getRespostas(), e.getComentario(), e.getCriadoEm()))
                .toList();
    }
}
