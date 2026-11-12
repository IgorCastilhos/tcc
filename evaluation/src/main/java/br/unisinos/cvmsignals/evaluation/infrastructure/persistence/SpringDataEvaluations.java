package br.unisinos.cvmsignals.evaluation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataEvaluations extends JpaRepository<EvaluationEntity, UUID> {
}
