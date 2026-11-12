package br.unisinos.cvmsignals.evaluation.domain;

import java.util.List;
import java.util.Map;

/**
 * Instrumento TAM (Davis, 1989) do Quadro 4 da monografia: cinco afirmações
 * de facilidade de uso percebida (1–5) e cinco de utilidade percebida (6–10),
 * respondidas em escala Likert de cinco pontos.
 */
public final class TamQuestionnaire {

    public static final int TOTAL_STATEMENTS = 10;
    public static final int LIKERT_MIN = 1;
    public static final int LIKERT_MAX = 5;

    public enum Dimension {
        FACILIDADE_DE_USO,
        UTILIDADE_PERCEBIDA
    }

    private static final List<String> STATEMENTS = List.of(
            "A interface que apresenta os sinais financeiros é de fácil utilização.",
            "Compreendo com facilidade os sinais de sentimento, risco e eventos corporativos exibidos.",
            "A forma como as informações são organizadas torna sua leitura intuitiva.",
            "Não preciso de esforço excessivo para interpretar os resultados gerados pelo sistema.",
            "Aprender a utilizar a ferramenta para acompanhar as empresas foi simples.",
            "Os sinais extraídos ajudam-me a compreender melhor a situação das empresas analisadas.",
            "As informações apresentadas agilizam minha análise dos documentos regulatórios.",
            "As informações apresentadas contribuíram para decisões de investimento potencialmente mais vantajosas.",
            "Os sinais gerados destacam riscos e eventos relevantes que eu poderia não ter identificado por conta própria.",
            "De modo geral, considero a ferramenta útil para o acompanhamento de empresas listadas na B3.");

    private TamQuestionnaire() {
    }

    public static List<String> statements() {
        return STATEMENTS;
    }

    /** Afirmações 1–5 medem facilidade de uso; 6–10, utilidade percebida. */
    public static Dimension dimensionOf(int statementNumber) {
        requireStatementNumber(statementNumber);
        return statementNumber <= 5 ? Dimension.FACILIDADE_DE_USO : Dimension.UTILIDADE_PERCEBIDA;
    }

    public static void validateAnswers(Map<Integer, Integer> answers) {
        if (answers.size() != TOTAL_STATEMENTS) {
            throw new IllegalArgumentException(
                    "O instrumento exige respostas às " + TOTAL_STATEMENTS + " afirmações; recebidas: " + answers.size());
        }
        answers.forEach((statement, likert) -> {
            requireStatementNumber(statement);
            if (likert == null || likert < LIKERT_MIN || likert > LIKERT_MAX) {
                throw new IllegalArgumentException(
                        "Resposta da afirmação " + statement + " fora da escala Likert 1-5: " + likert);
            }
        });
    }

    private static void requireStatementNumber(int statementNumber) {
        if (statementNumber < 1 || statementNumber > TOTAL_STATEMENTS) {
            throw new IllegalArgumentException("Afirmação inexistente: " + statementNumber);
        }
    }
}
