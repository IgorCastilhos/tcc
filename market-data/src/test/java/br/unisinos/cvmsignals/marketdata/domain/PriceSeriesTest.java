package br.unisinos.cvmsignals.marketdata.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PriceSeriesTest {

    private PricePoint point(String date, double close) {
        return new PricePoint("PETR4", LocalDate.parse(date), BigDecimal.valueOf(close), 1000);
    }

    @Test
    void calculaRetornosSimplesOrdenandoPorData() {
        // Pontos fora de ordem: a série deve ordenar antes de calcular.
        PriceSeries series = new PriceSeries("PETR4", List.of(
                point("2026-01-03", 110.0),
                point("2026-01-01", 100.0),
                point("2026-01-02", 105.0)));

        List<Double> returns = series.dailyReturns();

        assertThat(returns).hasSize(2);
        assertThat(returns.get(0)).isCloseTo(0.05, within(1e-9));
        assertThat(returns.get(1)).isCloseTo(110.0 / 105.0 - 1.0, within(1e-9));
    }

    @Test
    void retornosLogSaoAditivosNoTempo() {
        PriceSeries series = new PriceSeries("PETR4", List.of(
                point("2026-01-01", 100.0),
                point("2026-01-02", 105.0),
                point("2026-01-03", 110.0)));

        double soma = series.logReturns().stream().mapToDouble(Double::doubleValue).sum();

        assertThat(soma).isCloseTo(Math.log(110.0 / 100.0), within(1e-9));
    }

    @Test
    void serieVaziaOuUnitariaNaoTemRetornos() {
        assertThat(new PriceSeries("PETR4", List.of()).dailyReturns()).isEmpty();
        assertThat(new PriceSeries("PETR4", List.of(point("2026-01-01", 100.0))).dailyReturns()).isEmpty();
    }

    @Test
    void rejeitaPrecoNaoPositivo() {
        assertThatThrownBy(() -> point("2026-01-01", 0.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
