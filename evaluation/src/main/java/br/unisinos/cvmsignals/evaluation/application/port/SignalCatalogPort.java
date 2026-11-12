package br.unisinos.cvmsignals.evaluation.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Visão somente-leitura dos sinais extraídos, com dados de empresa,
 * documento e contextos — o que a interface web apresenta aos
 * participantes e o que a exportação RAGAS consome.
 */
public interface SignalCatalogPort {

    List<SignalView> listSignals();

    record SignalView(
            UUID signalId,
            String companyName,
            String cnpj,
            String documentType,
            LocalDate referenceDate,
            String model,
            String signalType,
            String label,
            double confidence,
            String justification,
            String evidence,
            List<String> contexts) {
    }
}
