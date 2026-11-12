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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Coração do TCC: para cada documento, recupera contexto via RAG e extrai
 * sentimento, risco e eventos corporativos com os três LLMs sob o MESMO
 * conjunto de prompts e contexto — condição necessária para o comparativo
 * ser válido. Falha de um provider não invalida os demais braços.
 */
public class ExtractSignalsService {

    /** Consultas fixas por tipo de sinal garantem contexto comparável entre modelos. */
    private static final Map<SignalType, String> QUERIES = Map.of(
            SignalType.SENTIMENTO, "situação financeira, resultados, desempenho e perspectivas da companhia",
            SignalType.RISCO, "riscos, contingências, incertezas, endividamento e fatores adversos",
            SignalType.EVENTO_CORPORATIVO,
            "aquisições, fusões, emissões, dividendos, mudanças de controle ou de administração");

    private final DocumentContextPort documentContext;
    private final LlmExtractionPort llm;
    private final SignalRepositoryPort signals;
    private final SignalEventPublisherPort events;
    private final List<LlmProvider> enabledProviders;
    private final int contextTopK;

    public ExtractSignalsService(DocumentContextPort documentContext, LlmExtractionPort llm,
                                 SignalRepositoryPort signals, SignalEventPublisherPort events,
                                 List<LlmProvider> enabledProviders, int contextTopK) {
        this.documentContext = documentContext;
        this.llm = llm;
        this.signals = signals;
        this.events = events;
        this.enabledProviders = List.copyOf(enabledProviders);
        this.contextTopK = contextTopK;
    }

    public int extract(UUID documentId) {
        DocumentContextPort.DocumentInfo info = documentContext.documentInfo(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não indexado: " + documentId));

        int total = 0;
        for (LlmProvider provider : enabledProviders) {
            List<FinancialSignal> extracted = new ArrayList<>();
            for (SignalType type : SignalType.values()) {
                List<String> contexts = documentContext.topChunks(documentId, QUERIES.get(type), contextTopK);
                if (contexts.isEmpty()) {
                    continue;
                }
                String contextBlock = String.join("\n\n---\n\n", contexts);
                for (SignalDraft draft : llm.extract(provider, type, info.companyName(), contextBlock)) {
                    extracted.add(new FinancialSignal(UUID.randomUUID(), documentId, provider, type,
                            draft.label(), draft.confidence(), draft.justification(),
                            draft.evidence(), contexts, Instant.now()));
                }
            }
            if (!extracted.isEmpty()) {
                signals.saveAll(extracted);
                events.publish(new SignalsExtracted(documentId, provider.name(), extracted.size()));
                total += extracted.size();
            }
        }
        return total;
    }
}
