package br.unisinos.cvmsignals.evaluation.application.port;

import br.unisinos.cvmsignals.evaluation.domain.UserEvaluation;

import java.util.List;

public interface EvaluationRepositoryPort {

    void save(UserEvaluation evaluation);

    List<UserEvaluation> findAll();
}
