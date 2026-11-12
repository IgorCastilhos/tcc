package br.unisinos.cvmsignals.ingestion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataDocuments extends JpaRepository<DocumentEntity, UUID> {

    boolean existsByUrlOrigem(String urlOrigem);
}
