package br.unisinos.cvmsignals.signalextraction.application.port;

import br.unisinos.cvmsignals.signalextraction.application.SignalDraft;
import br.unisinos.cvmsignals.signalextraction.domain.LlmProvider;
import br.unisinos.cvmsignals.signalextraction.domain.SignalType;

import java.util.List;

/**
 * Porta de saída para os LLMs. O caso de uso depende apenas desta
 * abstração — nunca de SDKs concretos —, cumprindo o DIP e permitindo
 * comparar Claude, GPT-4o e Gemma sob o mesmo contrato (Spring AI por trás).
 */
public interface LlmExtractionPort {

    List<SignalDraft> extract(LlmProvider provider, SignalType type, String companyName, String context);
}
