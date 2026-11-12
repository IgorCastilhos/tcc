package br.unisinos.cvmsignals.evaluation.application.usecase;

import br.unisinos.cvmsignals.evaluation.application.port.EvaluationRepositoryPort;
import br.unisinos.cvmsignals.evaluation.domain.TamQuestionnaire;
import br.unisinos.cvmsignals.evaluation.domain.UserEvaluation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consolidação descritiva das respostas (Seção 4.5): médias por afirmação,
 * por dimensão TAM e por semana, base do comparativo entre os três LLMs.
 */
public class EvaluationReportService {

    private final EvaluationRepositoryPort evaluations;

    public EvaluationReportService(EvaluationRepositoryPort evaluations) {
        this.evaluations = evaluations;
    }

    public Report generate() {
        List<UserEvaluation> all = evaluations.findAll();

        Map<Integer, Double> byStatement = new TreeMap<>();
        for (int statement = 1; statement <= TamQuestionnaire.TOTAL_STATEMENTS; statement++) {
            final int number = statement;
            byStatement.put(statement, all.stream()
                    .mapToInt(e -> e.answers().get(number))
                    .average()
                    .orElse(0.0));
        }

        Map<String, Double> byDimension = new LinkedHashMap<>();
        for (TamQuestionnaire.Dimension dimension : TamQuestionnaire.Dimension.values()) {
            byDimension.put(dimension.name(), all.stream()
                    .mapToDouble(e -> e.averageFor(dimension))
                    .average()
                    .orElse(0.0));
        }

        Map<Integer, Long> byWeek = new TreeMap<>();
        all.stream()
                .sorted(Comparator.comparingInt(UserEvaluation::week))
                .forEach(e -> byWeek.merge(e.week(), 1L, Long::sum));

        List<String> comments = all.stream()
                .map(UserEvaluation::comments)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        return new Report(all.size(), byStatement, byDimension, byWeek, comments);
    }

    public record Report(int totalResponses,
                         Map<Integer, Double> averageByStatement,
                         Map<String, Double> averageByDimension,
                         Map<Integer, Long> responsesByWeek,
                         List<String> comments) {
    }
}
