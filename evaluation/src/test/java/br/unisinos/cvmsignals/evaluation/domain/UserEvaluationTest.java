package br.unisinos.cvmsignals.evaluation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEvaluationTest {

    private Map<Integer, Integer> allAnswers(int value) {
        Map<Integer, Integer> answers = new HashMap<>();
        IntStream.rangeClosed(1, TamQuestionnaire.TOTAL_STATEMENTS).forEach(i -> answers.put(i, value));
        return answers;
    }

    @Test
    void exigeAsDezAfirmacoesDoQuadro4() {
        Map<Integer, Integer> incompleto = allAnswers(3);
        incompleto.remove(10);

        assertThatThrownBy(() -> new UserEvaluation(UUID.randomUUID(), "P01", 1, incompleto, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10");
    }

    @Test
    void rejeitaRespostaForaDaEscalaLikert() {
        Map<Integer, Integer> answers = allAnswers(3);
        answers.put(4, 6);

        assertThatThrownBy(() -> new UserEvaluation(UUID.randomUUID(), "P01", 1, answers, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaSemanaForaDoProtocoloDeTresSemanas() {
        assertThatThrownBy(() -> new UserEvaluation(UUID.randomUUID(), "P01", 4, allAnswers(3), null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculaMediaPorDimensaoTam() {
        Map<Integer, Integer> answers = allAnswers(2);
        IntStream.rangeClosed(6, 10).forEach(i -> answers.put(i, 5)); // utilidade máxima

        UserEvaluation evaluation = new UserEvaluation(UUID.randomUUID(), "P01", 2, answers, "ok", Instant.now());

        assertThat(evaluation.averageFor(TamQuestionnaire.Dimension.FACILIDADE_DE_USO)).isEqualTo(2.0);
        assertThat(evaluation.averageFor(TamQuestionnaire.Dimension.UTILIDADE_PERCEBIDA)).isEqualTo(5.0);
    }

    @Test
    void afirmacoesUmACincoMedemFacilidadeESeisADezUtilidade() {
        assertThat(TamQuestionnaire.dimensionOf(1)).isEqualTo(TamQuestionnaire.Dimension.FACILIDADE_DE_USO);
        assertThat(TamQuestionnaire.dimensionOf(5)).isEqualTo(TamQuestionnaire.Dimension.FACILIDADE_DE_USO);
        assertThat(TamQuestionnaire.dimensionOf(6)).isEqualTo(TamQuestionnaire.Dimension.UTILIDADE_PERCEBIDA);
        assertThat(TamQuestionnaire.statements()).hasSize(10);
    }
}
