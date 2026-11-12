package br.unisinos.cvmsignals.ingestion.infrastructure.persistence;

import br.unisinos.cvmsignals.ingestion.application.port.CompanyRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort;
import br.unisinos.cvmsignals.ingestion.domain.Company;
import br.unisinos.cvmsignals.ingestion.domain.RegulatoryDocument;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Traduz agregados de domínio para entidades JPA, mantendo o domínio livre de anotações. */
@Component
@Transactional
public class IngestionPersistenceAdapter implements CompanyRepositoryPort, DocumentRepositoryPort {

    private final SpringDataCompanies companies;
    private final SpringDataDocuments documents;
    private final EntityManager entityManager;

    public IngestionPersistenceAdapter(SpringDataCompanies companies, SpringDataDocuments documents,
                                       EntityManager entityManager) {
        this.companies = companies;
        this.documents = documents;
        this.entityManager = entityManager;
    }

    @Override
    public Company upsert(Company company) {
        CompanyEntity entity = companies.findByCnpj(company.cnpj())
                .map(existing -> {
                    existing.setNome(company.name());
                    existing.setCodigoCvm(company.cvmCode());
                    return existing;
                })
                .orElseGet(() -> new CompanyEntity(
                        company.id(), company.cnpj(), company.cvmCode(), company.name(), company.ticker()));
        CompanyEntity saved = companies.save(entity);
        return new Company(saved.getId(), saved.getCnpj(), saved.getCodigoCvm(), saved.getNome(), saved.getTicker());
    }

    @Override
    public Optional<Company> findByCnpj(String cnpj) {
        return companies.findByCnpj(Company.normalizeCnpj(cnpj))
                .map(e -> new Company(e.getId(), e.getCnpj(), e.getCodigoCvm(), e.getNome(), e.getTicker()));
    }

    @Override
    public void save(RegulatoryDocument doc) {
        documents.save(new DocumentEntity(doc.id(), doc.companyId(), doc.type(), doc.referenceDate(),
                doc.sourceUrl(), doc.protocol(), doc.status(), doc.text()));
    }

    @Override
    public Optional<RegulatoryDocument> findById(UUID id) {
        return documents.findById(id).map(e -> RegulatoryDocument.restore(
                e.getId(), e.getEmpresaId(), e.getTipo(), e.getDataReferencia(),
                e.getUrlOrigem(), e.getProtocolo(), e.getStatus(), e.getTexto()));
    }

    @Override
    public boolean existsBySourceUrl(String sourceUrl) {
        return documents.existsByUrlOrigem(sourceUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FailedDocument> findFailed(int limit) {
        return entityManager.createQuery("""
                        SELECT d.id, e.cnpj
                        FROM DocumentEntity d JOIN CompanyEntity e ON e.id = d.empresaId
                        WHERE d.status = 'FAILED'
                        ORDER BY d.criadoEm
                        """, Object[].class)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(row -> new FailedDocument((UUID) row[0], (String) row[1]))
                .toList();
    }
}
