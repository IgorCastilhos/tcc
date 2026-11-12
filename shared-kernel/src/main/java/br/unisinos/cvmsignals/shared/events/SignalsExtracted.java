package br.unisinos.cvmsignals.shared.events;

import java.util.UUID;

/** Publicado em cvm.signals.extracted ao final da extração de sinais de um documento por um modelo. */
public record SignalsExtracted(UUID documentId, String model, int signalCount) {
}
