package br.unisinos.cvmsignals.ingestion.application.usecase;

import br.unisinos.cvmsignals.ingestion.application.port.DocumentDownloadPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentParserPort;
import br.unisinos.cvmsignals.ingestion.application.port.DocumentRepositoryPort;
import br.unisinos.cvmsignals.ingestion.application.port.IngestionEventPublisherPort;
import br.unisinos.cvmsignals.ingestion.domain.RegulatoryDocument;
import br.unisinos.cvmsignals.ingestion.domain.DocumentStatus;
import br.unisinos.cvmsignals.shared.DocumentType;
import br.unisinos.cvmsignals.shared.events.DocumentDiscovered;
import br.unisinos.cvmsignals.shared.events.DocumentParsed;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O reprocessamento existe porque o ENET recusa rajadas: um documento que
 * falhou costuma baixar normalmente numa segunda tentativa. O que importa é
 * que uma falha isolada não interrompa o lote.
 */
class RetryFailedDocumentsServiceTest {

    private static final String CNPJ = "33000167000101";

    private final InMemoryDocuments documents = new InMemoryDocuments();
    private final FlakyDownloader downloader = new FlakyDownloader();
    private final RecordingEvents events = new RecordingEvents();

    private RetryFailedDocumentsService service() {
        ParseDocumentService parse = new ParseDocumentService(
                documents, downloader, bytes -> new String(bytes, StandardCharsets.UTF_8), events);
        return new RetryFailedDocumentsService(documents, parse, 0);
    }

    @Test
    void recuperaDocumentosQueVoltamAResponder() {
        UUID a = documents.addFailed("url-a");
        UUID b = documents.addFailed("url-b");

        RetryFailedDocumentsService.Result result = service().retry(10);

        assertThat(result.tentados()).isEqualTo(2);
        assertThat(result.recuperados()).isEqualTo(2);
        assertThat(result.aindaFalhando()).isZero();
        assertThat(documents.byId(a).status()).isEqualTo(DocumentStatus.PARSED);
        assertThat(documents.byId(b).status()).isEqualTo(DocumentStatus.PARSED);
    }

    /** Uma URL que segue recusando não pode abortar o restante do lote. */
    @Test
    void umaFalhaNaoInterrompeOLote() {
        documents.addFailed("url-a");
        UUID ruim = documents.addFailed("url-quebrada");
        documents.addFailed("url-c");
        downloader.alwaysFail.add("url-quebrada");

        RetryFailedDocumentsService.Result result = service().retry(10);

        assertThat(result.tentados()).isEqualTo(3);
        assertThat(result.recuperados()).isEqualTo(2);
        assertThat(result.aindaFalhando()).isEqualTo(1);
        assertThat(documents.byId(ruim).status()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    void respeitaOLimiteDoLote() {
        documents.addFailed("url-a");
        documents.addFailed("url-b");
        documents.addFailed("url-c");

        RetryFailedDocumentsService.Result result = service().retry(2);

        assertThat(result.tentados()).isEqualTo(2);
        assertThat(result.recuperados()).isEqualTo(2);
    }

    @Test
    void naoFazNadaQuandoNaoHaFalhados() {
        RetryFailedDocumentsService.Result result = service().retry(10);

        assertThat(result.tentados()).isZero();
        assertThat(result.recuperados()).isZero();
        assertThat(result.aindaFalhando()).isZero();
    }

    // ------------------------------------------------------------------ fakes

    private static final class FlakyDownloader implements DocumentDownloadPort {
        final Set<String> alwaysFail = new HashSet<>();

        @Override
        public byte[] download(String url) {
            if (alwaysFail.contains(url)) {
                throw new IllegalStateException("ENET recusou: " + url);
            }
            // RegulatoryDocument.markParsed recusa texto com menos de 100
            // caracteres, por tratar isso como falha de parsing.
            return ("Texto do documento regulatório em " + url + ". ").repeat(6)
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private static final class InMemoryDocuments implements DocumentRepositoryPort {
        private final Map<UUID, RegulatoryDocument> byId = new HashMap<>();
        private final List<FailedDocument> failed = new ArrayList<>();

        UUID addFailed(String url) {
            UUID id = UUID.randomUUID();
            RegulatoryDocument doc = RegulatoryDocument.restore(
                    id, UUID.randomUUID(), DocumentType.FATO_RELEVANTE,
                    LocalDate.of(2026, 2, 11), url, "protocolo", DocumentStatus.FAILED, null);
            byId.put(id, doc);
            failed.add(new FailedDocument(id, CNPJ));
            return id;
        }

        RegulatoryDocument byId(UUID id) {
            return byId.get(id);
        }

        @Override
        public void save(RegulatoryDocument document) {
            byId.put(document.id(), document);
        }

        @Override
        public Optional<RegulatoryDocument> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public boolean existsBySourceUrl(String sourceUrl) {
            return false;
        }

        @Override
        public List<FailedDocument> findFailed(int limit) {
            return failed.stream().limit(limit).toList();
        }
    }

    private static final class RecordingEvents implements IngestionEventPublisherPort {
        final List<DocumentParsed> parsed = new ArrayList<>();

        @Override
        public void publish(DocumentDiscovered event) {
            // irrelevante para o reprocessamento
        }

        @Override
        public void publish(DocumentParsed event) {
            parsed.add(event);
        }
    }
}
