package br.unisinos.cvmsignals.signalextraction.application;

/** Saída estruturada bruta de um LLM, antes de virar agregado FinancialSignal. */
public record SignalDraft(String label, double confidence, String justification, String evidence) {
}
