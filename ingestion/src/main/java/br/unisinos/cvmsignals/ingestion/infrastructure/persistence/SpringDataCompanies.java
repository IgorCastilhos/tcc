package br.unisinos.cvmsignals.ingestion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCompanies extends JpaRepository<CompanyEntity, UUID> {

    Optional<CompanyEntity> findByCnpj(String cnpj);
}
