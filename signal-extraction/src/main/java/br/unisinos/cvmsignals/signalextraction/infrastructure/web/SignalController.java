package br.unisinos.cvmsignals.signalextraction.infrastructure.web;

import br.unisinos.cvmsignals.signalextraction.application.port.SignalRepositoryPort;
import br.unisinos.cvmsignals.signalextraction.application.usecase.ExtractSignalsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/signals")
public class SignalController {

    private final ExtractSignalsService extractSignals;
    private final SignalRepositoryPort signals;

    public SignalController(ExtractSignalsService extractSignals, SignalRepositoryPort signals) {
        this.extractSignals = extractSignals;
        this.signals = signals;
    }

    /** Dispara a extração dos três braços para um documento já indexado. */
    @PostMapping("/extract/{documentId}")
    public Map<String, Object> extract(@PathVariable UUID documentId) {
        return Map.of("documentId", documentId, "sinaisExtraidos", extractSignals.extract(documentId));
    }

    @GetMapping("/document/{documentId}")
    public List<SignalResponse> byDocument(@PathVariable UUID documentId) {
        return signals.findByDocumentId(documentId).stream()
                .map(s -> new SignalResponse(s.id(), s.documentId(), s.model().name(), s.type().name(),
                        s.label(), s.confidence(), s.justification(), s.evidence(), s.createdAt()))
                .toList();
    }

    /** DTO de resposta: o agregado de domínio não expõe convenções JavaBean ao Jackson. */
    public record SignalResponse(UUID id, UUID documentId, String model, String type, String label,
                                 double confidence, String justification, String evidence, Instant createdAt) {
    }
}
