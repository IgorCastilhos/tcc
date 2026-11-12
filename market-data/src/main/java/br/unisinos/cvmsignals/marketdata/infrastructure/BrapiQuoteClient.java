package br.unisinos.cvmsignals.marketdata.infrastructure;

import br.unisinos.cvmsignals.marketdata.application.port.QuoteProviderPort;
import br.unisinos.cvmsignals.marketdata.domain.PricePoint;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Cliente do brapi.dev (plano gratuito), fonte de cotações históricas da B3
 * definida na Seção 4.4. Timestamps vêm em epoch seconds no fuso de São Paulo.
 */
@Component
public class BrapiQuoteClient implements QuoteProviderPort {

    private static final ZoneId B3_ZONE = ZoneId.of("America/Sao_Paulo");

    private final RestClient brapiRestClient;
    private final String token;

    public BrapiQuoteClient(@Qualifier("brapiRestClient") RestClient brapiRestClient,
                            @Value("${brapi.token:}") String token) {
        this.brapiRestClient = brapiRestClient;
        this.token = token;
    }

    @Override
    public List<PricePoint> fetchDailyHistory(String ticker, String range) {
        BrapiResponse response = brapiRestClient.get()
                .uri(uri -> uri.path("/api/quote/{ticker}")
                        .queryParam("range", range)
                        .queryParam("interval", "1d")
                        .queryParam("token", token)
                        .build(ticker))
                .retrieve()
                .body(BrapiResponse.class);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new IllegalStateException("brapi.dev sem resultados para " + ticker);
        }
        List<HistoricalPrice> history = response.results().get(0).historicalDataPrice();
        if (history == null) {
            return List.of();
        }
        return history.stream()
                .filter(h -> h.close() != null)
                .map(h -> new PricePoint(
                        ticker.toUpperCase(),
                        LocalDate.ofInstant(Instant.ofEpochSecond(h.date()), B3_ZONE),
                        BigDecimal.valueOf(h.close()),
                        h.volume() == null ? 0L : h.volume()))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BrapiResponse(List<BrapiResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BrapiResult(List<HistoricalPrice> historicalDataPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HistoricalPrice(long date, Double close, Long volume) {
    }
}
