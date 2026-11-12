package br.unisinos.cvmsignals.marketdata.application.port;

import br.unisinos.cvmsignals.marketdata.domain.PricePoint;

import java.util.List;

/** Porta de saída para o provedor de cotações da B3 (brapi.dev). */
public interface QuoteProviderPort {

    List<PricePoint> fetchDailyHistory(String ticker, String range);
}
