package br.unisinos.cvmsignals.marketdata.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Série de preços ordenada por data, com cálculo de retornos simples e
 * logarítmicos — a base quantitativa que contextualiza os sinais na
 * interface de avaliação e em análises posteriores de retornos anormais.
 */
public final class PriceSeries {

    private final String ticker;
    private final List<PricePoint> points;

    public PriceSeries(String ticker, List<PricePoint> points) {
        this.ticker = ticker;
        List<PricePoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparing(PricePoint::date));
        this.points = List.copyOf(sorted);
    }

    /** Retornos simples diários: r_t = p_t / p_{t-1} - 1. */
    public List<Double> dailyReturns() {
        List<Double> returns = new ArrayList<>(Math.max(0, points.size() - 1));
        for (int i = 1; i < points.size(); i++) {
            double previous = points.get(i - 1).close().doubleValue();
            double current = points.get(i).close().doubleValue();
            returns.add(current / previous - 1.0);
        }
        return returns;
    }

    /** Retornos logarítmicos diários: r_t = ln(p_t / p_{t-1}), aditivos no tempo. */
    public List<Double> logReturns() {
        List<Double> returns = new ArrayList<>(Math.max(0, points.size() - 1));
        for (int i = 1; i < points.size(); i++) {
            double previous = points.get(i - 1).close().doubleValue();
            double current = points.get(i).close().doubleValue();
            returns.add(Math.log(current / previous));
        }
        return returns;
    }

    public String ticker() { return ticker; }
    public List<PricePoint> points() { return points; }
    public boolean isEmpty() { return points.isEmpty(); }
}
