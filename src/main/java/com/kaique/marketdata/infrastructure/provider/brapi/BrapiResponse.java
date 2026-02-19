package com.kaique.marketdata.infrastructure.provider.brapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Record que mapeia a resposta JSON da Brapi API.
 *
 * Para cotação atual: GET /api/quote/{symbol}?token={token}
 * Para histórico:     GET /api/quote/{symbol}?range=1mo&interval=1d&token={token}
 *
 * O campo historicalDataPrice aparece quando range/interval são especificados:
 * {
 *   "results": [{
 *     "symbol": "PETR4",
 *     "shortName": "PETROBRAS PN N2",
 *     "currency": "BRL",
 *     "regularMarketPrice": 38.15,
 *     "marketCap": 500000000000,
 *     "historicalDataPrice": [
 *       { "date": 1769731200, "open": 37.23, "high": 37.98, "low": 37.02, "close": 37.76, "volume": 45761300, "adjustedClose": 37.76 }
 *     ]
 *   }]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiResponse(
        List<BrapiQuote> results,
        String requestedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiQuote(
            String symbol,
            String shortName,
            String longName,
            String currency,
            BigDecimal regularMarketPrice,
            BigDecimal regularMarketDayHigh,
            BigDecimal regularMarketDayLow,
            BigDecimal regularMarketVolume,
            BigDecimal regularMarketChange,
            BigDecimal regularMarketChangePercent,
            BigDecimal marketCap,
            List<HistoricalDataPrice> historicalDataPrice
    ) {
    }

    /**
     * Representa um ponto de dados histórico da Brapi.
     * O campo "date" vem como Unix Epoch EM SEGUNDOS (não milissegundos).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HistoricalDataPrice(
            long date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal adjustedClose
    ) {
    }
}
