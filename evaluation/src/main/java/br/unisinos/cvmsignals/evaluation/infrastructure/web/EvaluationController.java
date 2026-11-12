package br.unisinos.cvmsignals.evaluation.infrastructure.web;

import br.unisinos.cvmsignals.evaluation.application.port.SignalCatalogPort;
import br.unisinos.cvmsignals.evaluation.application.usecase.EvaluationReportService;
import br.unisinos.cvmsignals.evaluation.application.usecase.SubmitEvaluationService;
import br.unisinos.cvmsignals.evaluation.domain.TamQuestionnaire;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final SubmitEvaluationService submitEvaluation;
    private final EvaluationReportService report;
    private final SignalCatalogPort catalog;

    public EvaluationController(SubmitEvaluationService submitEvaluation,
                                EvaluationReportService report, SignalCatalogPort catalog) {
        this.submitEvaluation = submitEvaluation;
        this.report = report;
        this.catalog = catalog;
    }

    /** Sinais + afirmações do Quadro 4, consumidos pela interface web do estudo. */
    @GetMapping("/signals")
    public Map<String, Object> signals() {
        return Map.of(
                "statements", TamQuestionnaire.statements(),
                "signals", catalog.listSignals());
    }

    @PostMapping
    public Map<String, Object> submit(@RequestBody SubmitRequest request) {
        UUID id = submitEvaluation.submit(
                request.participantCode(), request.week(), request.answers(), request.comments());
        return Map.of("id", id);
    }

    @GetMapping("/report")
    public EvaluationReportService.Report reportJson() {
        return report.generate();
    }

    /**
     * Dataset RAGAS em JSONL: question (consulta do sinal), answer
     * (justificativa do LLM) e contexts (trechos usados) — Seção 4.5.
     */
    @GetMapping(value = "/ragas-dataset", produces = MediaType.TEXT_PLAIN_VALUE)
    public String ragasDataset() {
        return catalog.listSignals().stream()
                .map(s -> """
                        {"question": %s, "answer": %s, "contexts": [%s], "model": "%s", "signal_type": "%s"}"""
                        .formatted(
                                jsonString(s.signalType() + " de " + s.companyName()),
                                jsonString(s.label() + ". " + nullSafe(s.justification())),
                                s.contexts().stream().map(this::jsonString).collect(Collectors.joining(",")),
                                s.model(),
                                s.signalType()))
                .collect(Collectors.joining("\n"));
    }

    private String jsonString(String value) {
        return "\"" + nullSafe(value).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public record SubmitRequest(String participantCode, int week,
                                Map<Integer, Integer> answers, String comments) {
    }
}
