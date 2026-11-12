package br.unisinos.cvmsignals.signalextraction.infrastructure.persistence;

import br.unisinos.cvmsignals.signalextraction.application.port.SignalRepositoryPort;
import br.unisinos.cvmsignals.signalextraction.domain.FinancialSignal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Transactional
public class SignalPersistenceAdapter implements SignalRepositoryPort {

    private final SpringDataSignals signals;

    public SignalPersistenceAdapter(SpringDataSignals signals) {
        this.signals = signals;
    }

    @Override
    public void saveAll(List<FinancialSignal> financialSignals) {
        signals.saveAll(financialSignals.stream()
                .map(s -> new SignalEntity(s.id(), s.documentId(), s.model(), s.type(), s.label(),
                        s.confidence(), s.justification(), s.evidence(), s.contexts(), s.createdAt()))
                .toList());
    }

    @Override
    public List<FinancialSignal> findByDocumentId(UUID documentId) {
        return signals.findByDocumentoId(documentId).stream()
                .map(e -> new FinancialSignal(e.getId(), e.getDocumentoId(), e.getModelo(), e.getTipo(),
                        e.getRotulo(), e.getConfianca(), e.getJustificativa(), e.getEvidencia(),
                        e.getContextos(), e.getCriadoEm()))
                .toList();
    }
}
