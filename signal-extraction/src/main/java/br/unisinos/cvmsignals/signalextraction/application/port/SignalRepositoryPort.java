package br.unisinos.cvmsignals.signalextraction.application.port;

import br.unisinos.cvmsignals.signalextraction.domain.FinancialSignal;

import java.util.List;
import java.util.UUID;

public interface SignalRepositoryPort {

    void saveAll(List<FinancialSignal> signals);

    List<FinancialSignal> findByDocumentId(UUID documentId);
}
