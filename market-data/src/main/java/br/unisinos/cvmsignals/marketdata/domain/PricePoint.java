package br.unisinos.cvmsignals.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Cotação diária de fechamento de um papel na B3. */
public record PricePoint(String ticker, LocalDate date, BigDecimal close, long volume) {

    public PricePoint {
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(close, "close");
        if (close.signum() <= 0) {
            throw new IllegalArgumentException("Fechamento deve ser positivo: " + close);
        }
    }
}
