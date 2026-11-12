package br.unisinos.cvmsignals.marketdata.infrastructure;

import br.unisinos.cvmsignals.marketdata.application.usecase.SyncPricesService;
import br.unisinos.cvmsignals.marketdata.domain.PriceSeries;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final SyncPricesService syncPrices;

    public MarketDataController(SyncPricesService syncPrices) {
        this.syncPrices = syncPrices;
    }

    /** Ex.: POST /api/market-data/sync/PETR4?range=1y */
    @PostMapping("/sync/{ticker}")
    public Map<String, Object> sync(@PathVariable String ticker,
                                    @RequestParam(defaultValue = "1y") String range) {
        return Map.of("ticker", ticker, "pontos", syncPrices.sync(ticker, range));
    }

    @GetMapping("/series/{ticker}")
    public Map<String, Object> series(@PathVariable String ticker) {
        PriceSeries series = syncPrices.series(ticker);
        return Map.of(
                "ticker", series.ticker(),
                "pontos", series.points(),
                "retornosDiarios", series.dailyReturns());
    }
}
