package com.kaique.marketdata.infrastructure.provider;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.TimeRange;
import com.kaique.marketdata.domain.model.Candle;
import com.kaique.marketdata.domain.model.MarketData;

import java.util.List;

/**
 * Interface Strategy para provedores de dados de mercado.
 */
public interface MarketDataProvider {

    /** Busca preço atual (CRYPTO, STOCK, FII). */
    MarketData fetchCurrentPrice(String symbol);

    /**
     * Busca histórico OHLCV normalizado.
     * @return List<Candle> com timestamp em epoch seconds (ordenado).
     */
    List<Candle> fetchHistory(String symbol, TimeRange timeRange);

    boolean supports(MarketType marketType);
}
