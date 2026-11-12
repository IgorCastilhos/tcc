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
import br.unisinos.cvmsignals.shared.events.DocumentParsed;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fakes em memória no lugar de mocks: o comportamento observável é o contrato.
 *
 * <p>O CSV da CVM vem agrupado por companhia — daí os cenários girarem em
 * torno de distribuir a coleta entre empresas em vez de esgotar o limite nas
 * primeiras da lista.
 */
class DiscoverDocumentsServiceTest {

    private static final String PETRO = "33000167000101";
    private static final String VALE = "33592510000154";
    private static final String ITAU = "60872504000123";

    private final InMemoryDocuments documents = new InMemoryDocuments();
    private final RecordingEvents events = new RecordingEvents();

    @Test
    void descobreTudoQuandoNaoHaFiltro() {
        DiscoverDocumentsService service = serviceWith(
                doc(PETRO, "url-1"), doc(VALE, "url-2"), doc(ITAU, "url-3"));

        int published = service.discover(DocumentType.FATO_RELEVANTE, 2026, 10);

        assertThat(published).isEqualTo(3);
        assertThat(events.published).hasSize(3);
    }

    @Test
    void ignoraCompanhiasForaDoFiltroDeCnpj() {
        DiscoverDocumentsService service = serviceWith(
                doc(PETRO, "url-1"), doc(VALE, "url-2"), doc(ITAU, "url-3"));

        int published = service.discover(
                DocumentType.FATO_RELEVANTE, 2026, 10, Set.of(PETRO, ITAU), Integer.MAX_VALUE);

        assertThat(published).isEqualTo(2);
        assertThat(events.published).extracting(DocumentDiscovered::cnpj)
                .containsExactlyInAnyOrder(PETRO, ITAU);
    }

    @Test
    void aceitaCnpjComPontuacaoNoFiltro() {
        DiscoverDocumentsService service = serviceWith(doc(PETRO, "url-1"), doc(VALE, "url-2"));

        int published = service.discover(
                DocumentType.FATO_RELEVANTE, 2026, 10, Set.of("33.000.167/0001-01"), Integer.MAX_VALUE);

        assertThat(published).isEqualTo(1);
        assertThat(events.published).singleElement()
                .extracting(DocumentDiscovered::cnpj).isEqualTo(PETRO);
    }

    /** Sem teto por empresa, a primeira companhia do CSV consumiria o limite inteiro. */
    @Test
    void distribuiEntreEmpresasQuandoHaTetoPorCompanhia() {
        DiscoverDocumentsService service = serviceWith(
                doc(PETRO, "url-1"), doc(PETRO, "url-2"), doc(PETRO, "url-3"),
                doc(VALE, "url-4"), doc(ITAU, "url-5"));

        int published = service.discover(DocumentType.FATO_RELEVANTE, 2026, 10, Set.of(), 1);

        assertThat(published).isEqualTo(3);
        assertThat(events.published).extracting(DocumentDiscovered::cnpj)
                .containsExactlyInAnyOrder(PETRO, VALE, ITAU);
    }

    @Test
    void respeitaOLimiteGlobal() {
        DiscoverDocumentsService service = serviceWith(
                doc(PETRO, "url-1"), doc(VALE, "url-2"), doc(ITAU, "url-3"));

        int published = service.discover(DocumentType.FATO_RELEVANTE, 2026, 2);

        assertThat(published).isEqualTo(2);
    }

    /** Idempotência: redescobrir não republica o que já foi ingerido. */
    @Test
    void ignoraDocumentoJaConhecidoPelaUrl() {
        documents.knownUrls.add("url-2");
        DiscoverDocumentsService service = serviceWith(doc(PETRO, "url-1"), doc(VALE, "url-2"));

        int published = service.discover(DocumentType.FATO_RELEVANTE, 2026, 10);

        assertThat(published).isEqualTo(1);
        assertThat(events.published).singleElement()
                .extracting(DocumentDiscovered::cnpj).isEqualTo(PETRO);
    }

    /** O teto conta apenas a execução corrente, não o histórico do banco. */
    @Test
    void tetoPorEmpresaNaoBloqueiaDocumentoDeOutraCompanhia() {
        DiscoverDocumentsService service = serviceWith(
                doc(PETRO, "url-1"), doc(PETRO, "url-2"), doc(VALE, "url-3"));

        int published = service.discover(DocumentType.FATO_RELEVANTE, 2026, 10, Set.of(), 1);

        assertThat(published).isEqualTo(2);
        assertThat(events.published).extracting(DocumentDiscovered::cnpj)
                .containsExactlyInAnyOrder(PETRO, VALE);
    }

    // ------------------------------------------------------------------ fakes

    private DiscoverDocumentsService serviceWith(DiscoveredDocument... discovered) {
        CvmPortalPort portal = (type, year) -> List.of(discovered);
        return new DiscoverDocumentsService(portal, new InMemoryCompanies(), documents, events);
    }

    private static DiscoveredDocument doc(String cnpj, String url) {
        return new DiscoveredDocument(cnpj, "Companhia " + cnpj, "1234",
                DocumentType.FATO_RELEVANTE, LocalDate.of(2026, 2, 11), url, "protocolo-" + url);
    }

    private static final class InMemoryCompanies implements CompanyRepositoryPort {
        @Override
        public Company upsert(Company company) {
            return company;
        }

        @Override
        public Optional<Company> findByCnpj(String cnpj) {
            return Optional.empty();
        }
    }

    private static final class InMemoryDocuments implements DocumentRepositoryPort {
        final Set<String> knownUrls = new HashSet<>();
        final List<RegulatoryDocument> saved = new ArrayList<>();

        @Override
        public void save(RegulatoryDocument document) {
            saved.add(document);
        }

        @Override
        public Optional<RegulatoryDocument> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public boolean existsBySourceUrl(String sourceUrl) {
            return knownUrls.contains(sourceUrl);
        }

        @Override
        public List<FailedDocument> findFailed(int limit) {
            return List.of();
        }
    }

    private static final class RecordingEvents implements IngestionEventPublisherPort {
        final List<DocumentDiscovered> published = new ArrayList<>();

        @Override
        public void publish(DocumentDiscovered event) {
            published.add(event);
        }

        @Override
        public void publish(DocumentParsed event) {
            // irrelevante para a descoberta
        }
    }
}
