package br.unisinos.cvmsignals.ingestion.application.usecase;

import br.unisinos.cvmsignals.ingestion.application.DiscoveredDocument;
import br.unisinos.cvmsignals.ingestion.application.port.CompanyRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.CvmPortalPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.IngestionEventPublisherPort;
import br.unisinos.cvmsignals.ingestion.domain.Company;
import br.unisinos.cvmsignals.ingestion.domain.RegulatoryDocument;
import br.unisinos.cvmsignals.shared.DocumentType;
import br.unisinos.cvmsignals.shared.events.DocumentDiscovered;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso: descobrir documentos na CVM, registrar empresa e documento
 * e publicar o evento cvm.docs.discovered. A idempotência é garantida pela
 * URL de origem: documentos já conhecidos são ignorados.
 *
 * <p>O CSV da CVM vem agrupado por companhia, então uma descoberta sem
 * filtro esgota o limite nas primeiras empresas da lista. Para o estudo
 * interessa o oposto — cobrir várias companhias da B3 —, daí o filtro por
 * CNPJ e o teto por empresa.
 */
public class DiscoverDocumentsService {

    private final CvmPortalPort cvmPortal;
    private final CompanyRepositoryPort companies;
    private final DocumentRepositoryPort documents;
    private final IngestionEventPublisherPort events;

    public DiscoverDocumentsService(CvmPortalPort cvmPortal, CompanyRepositoryPort companies,
                                    DocumentRepositoryPort documents, IngestionEventPublisherPort events) {
        this.cvmPortal = cvmPortal;
        this.companies = companies;
        this.documents = documents;
        this.events = events;
    }

    public int discover(DocumentType type, int year, int limit) {
        return discover(type, year, limit, Set.of(), Integer.MAX_VALUE);
    }

    /**
     * @param cnpjs         quando não vazio, só descobre documentos destas
     *                      companhias (aceita CNPJ com ou sem pontuação)
     * @param maxPerCompany teto de documentos por companhia nesta execução,
     *                      para distribuir o limite entre várias empresas
     */
    public int discover(DocumentType type, int year, int limit, Set<String> cnpjs, int maxPerCompany) {
        Set<String> wanted = cnpjs.stream()
                .map(Company::normalizeCnpj)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, Integer> perCompany = new HashMap<>();

        List<DiscoveredDocument> discovered = cvmPortal.discover(type, year);
        int published = 0;
        for (DiscoveredDocument meta : discovered) {
            if (published >= limit) {
                break;
            }
            String cnpj = Company.normalizeCnpj(meta.cnpj());
            if (!wanted.isEmpty() && !wanted.contains(cnpj)) {
                continue;
            }
            if (perCompany.getOrDefault(cnpj, 0) >= maxPerCompany) {
                continue;
            }
            if (documents.existsBySourceUrl(meta.downloadUrl())) {
                continue;
            }
            perCompany.merge(cnpj, 1, Integer::sum);
            Company company = companies.upsert(new Company(
                    UUID.randomUUID(), meta.cnpj(), meta.cvmCode(), meta.companyName(), null));
            RegulatoryDocument doc = new RegulatoryDocument(
                    UUID.randomUUID(), company.id(), meta.type(),
                    meta.referenceDate(), meta.downloadUrl(), meta.protocol());
            documents.save(doc);
            events.publish(new DocumentDiscovered(
                    doc.id(), company.cnpj(), company.name(), doc.type(),
                    doc.referenceDate(), doc.sourceUrl()));
            published++;
        }
        return published;
    }
}
