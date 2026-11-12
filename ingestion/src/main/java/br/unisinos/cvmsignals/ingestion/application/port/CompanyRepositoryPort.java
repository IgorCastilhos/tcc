package br.unisinos.cvmsignals.ingestion.application.port;

import br.unisinos.cvmsignals.ingestion.domain.Company;

import java.util.Optional;

public interface CompanyRepositoryPort {

    Company upsert(Company company);

    Optional<Company> findByCnpj(String cnpj);
}
