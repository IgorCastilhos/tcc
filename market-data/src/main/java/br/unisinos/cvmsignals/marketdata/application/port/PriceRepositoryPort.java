package br.unisinos.cvmsignals.marketdata.application.port;

import br.unisinos.cvmsignals.marketdata.domain.PricePoint;
import br.unisinos.cvmsignals.marketdata.domain.PriceSeries;

import java.util.List;

public interface PriceRepositoryPort {

    void upsertAll(List<PricePoint> points);

    PriceSeries findSeries(String ticker);
}
