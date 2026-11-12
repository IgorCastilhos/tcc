package br.unisinos.cvmsignals.marketdata.infrastructure;

import br.unisinos.cvmsignals.marketdata.application.port.PriceRepositoryPort;
import br.unisinos.cvmsignals.marketdata.domain.PricePoint;
import br.unisinos.cvmsignals.marketdata.domain.PriceSeries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcPriceRepository implements PriceRepositoryPort {

    private static final String UPSERT = """
            INSERT INTO serie_preco (ticker, data, fechamento, volume)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (ticker, data) DO UPDATE SET fechamento = EXCLUDED.fechamento, volume = EXCLUDED.volume
            """;

    private final JdbcTemplate jdbc;

    public JdbcPriceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsertAll(List<PricePoint> points) {
        jdbc.batchUpdate(UPSERT, points, points.size(), (ps, point) -> {
            ps.setString(1, point.ticker());
            ps.setObject(2, point.date());
            ps.setBigDecimal(3, point.close());
            ps.setLong(4, point.volume());
        });
    }

    @Override
    public PriceSeries findSeries(String ticker) {
        List<PricePoint> points = jdbc.query(
                "SELECT ticker, data, fechamento, volume FROM serie_preco WHERE ticker = ? ORDER BY data",
                (rs, i) -> new PricePoint(
                        rs.getString("ticker"),
                        rs.getObject("data", LocalDate.class),
                        rs.getBigDecimal("fechamento"),
                        rs.getLong("volume")),
                ticker.toUpperCase());
        return new PriceSeries(ticker.toUpperCase(), points);
    }
}
