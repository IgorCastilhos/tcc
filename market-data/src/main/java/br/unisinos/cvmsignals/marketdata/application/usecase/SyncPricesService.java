package br.unisinos.cvmsignals.marketdata.application.usecase;

import br.unisinos.cvmsignals.marketdata.application.port.PriceRepositoryPort;
import br.unisinos.cvmsignals.marketdata.application.port.QuoteProviderPort;
import br.unisinos.cvmsignals.marketdata.domain.PricePoint;
import br.unisinos.cvmsignals.marketdata.domain.PriceSeries;

import java.util.List;

/**
 * Caso de uso: sincronizar o histórico de um ticker com o provedor e
 * persistir de forma idempotente (upsert por ticker+data), permitindo
 * reexecuções sem duplicar a série.
 */
public class SyncPricesService {

    private final QuoteProviderPort quotes;
    private final PriceRepositoryPort prices;

    public SyncPricesService(QuoteProviderPort quotes, PriceRepositoryPort prices) {
        this.quotes = quotes;
        this.prices = prices;
    }

    public int sync(String ticker, String range) {
        List<PricePoint> history = quotes.fetchDailyHistory(ticker, range);
        prices.upsertAll(history);
        return history.size();
    }

    public PriceSeries series(String ticker) {
        return prices.findSeries(ticker);
    }
}
