package br.unisinos.cvmsignals.signalextraction.application.usecase;

import br.unisinos.cvmsignals.signalextraction.application.SignalDraft;
import br.unisinos.cvmsignals.signalextraction.application.port.DocumentContextPort;
import br.unisinos.cvmsignals.signalextraction.application.port.LlmExtractionPort;
import br.unisinos.cvmsignals.signalextraction.application.port.SignalEventPublisherPort;
import br.unisinos.cvmsignals.signalextraction.application.port.SignalRepositoryPort;
import br.unisinos.cvmsignals.signalextraction.domain.FinancialSignal;
import br.unisinos.cvmsignals.signalextraction.domain.LlmProvider;
import br.unisinos.cvmsignals.signalextraction.domain.SignalType;
import br.unisinos.cvmsignals.shared.events.SignalsExtracted;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Fakes em memória no lugar de mocks: o comportamento observável é o contrato. */
class ExtractSignalsServiceTest {

    private final UUID documentId = UUID.randomUUID();

    private final DocumentContextPort context = new DocumentContextPort() {
        @Override
        public Optional<DocumentInfo> documentInfo(UUID id) {
            return Optional.of(new DocumentInfo(id, "00000000000191", "Petrobras", "FATO_RELEVANTE"));
        }

        @Override
        public List<String> topChunks(UUID id, String query, int topK) {
            return List.of("trecho A sobre " + query, "trecho B");
        }
    };

    private static final class InMemorySignals implements SignalRepositoryPort {
        final List<FinancialSignal> saved = new ArrayList<>();

        @Override
        public void saveAll(List<FinancialSignal> signals) {
            saved.addAll(signals);
        }

        @Override
        public List<FinancialSignal> findByDocumentId(UUID documentId) {
            return saved;
        }
    }

    private static final class CapturedEvents implements SignalEventPublisherPort {
        final List<SignalsExtracted> published = new ArrayList<>();

        @Override
        public void publish(SignalsExtracted event) {
            published.add(event);
        }
    }

    @Test
    void extraiOsTresTiposDeSinalParaCadaProviderHabilitado() {
        InMemorySignals signals = new InMemorySignals();
        CapturedEvents events = new CapturedEvents();
        LlmExtractionPort llm = (provider, type, company, ctx) ->
                List.of(new SignalDraft("positivo", 0.9, "justificativa", "evidência"));

        ExtractSignalsService service = new ExtractSignalsService(
                context, llm, signals, events, List.of(LlmProvider.CLAUDE_SONNET, LlmProvider.GEMMA), 5);

        int total = service.extract(documentId);

        // 2 providers × 3 tipos de sinal.
        assertThat(total).isEqualTo(6);
        assertThat(signals.saved).hasSize(6);
        assertThat(signals.saved).allSatisfy(s -> {
            assertThat(s.label()).isEqualTo("POSITIVO"); // invariante: rótulo normalizado
            assertThat(s.contexts()).isNotEmpty();       // insumo do RAGAS persistido
        });
        assertThat(events.published)
                .extracting(SignalsExtracted::model)
                .containsExactlyInAnyOrder("CLAUDE_SONNET", "GEMMA");
    }

    @Test
    void providerQueNaoRetornaSinaisNaoGeraEventoNemDerrubaOsDemais() {
        InMemorySignals signals = new InMemorySignals();
        CapturedEvents events = new CapturedEvents();
        // O adaptador converte falhas do provider em lista vazia; o caso de uso segue os demais braços.
        LlmExtractionPort llm = (provider, type, company, ctx) ->
                provider == LlmProvider.GEMMA
                        ? List.of()
                        : List.of(new SignalDraft("ALTO", 0.7, "j", "e"));

        ExtractSignalsService service = new ExtractSignalsService(
                context, llm, signals, events, List.of(LlmProvider.GPT_4O, LlmProvider.GEMMA), 5);

        int total = service.extract(documentId);

        assertThat(total).isEqualTo(3);
        assertThat(events.published).extracting(SignalsExtracted::model).containsExactly("GPT_4O");
    }

    @Test
    void eventoCorporativoPodeGerarMultiplosSinaisPorChamada() {
        InMemorySignals signals = new InMemorySignals();
        LlmExtractionPort llm = (provider, type, company, ctx) ->
                type == SignalType.EVENTO_CORPORATIVO
                        ? List.of(new SignalDraft("AQUISICAO", 0.8, "j1", "e1"),
                                  new SignalDraft("DIVIDENDOS", 0.6, "j2", "e2"))
                        : List.of(new SignalDraft("NEUTRO", 0.5, "j", "e"));

        ExtractSignalsService service = new ExtractSignalsService(
                context, llm, signals, new CapturedEvents(), List.of(LlmProvider.CLAUDE_SONNET), 5);

        assertThat(service.extract(documentId)).isEqualTo(4);
    }
}
