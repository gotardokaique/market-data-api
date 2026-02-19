package com.kaique.marketdata.infrastructure.provider.brapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Record que mapeia a resposta JSON da Brapi API.
 *
 * Exemplo de resposta da Brapi:
 * {
 *   "results": [{
 *     "symbol": "PETR4",
 *     "shortName": "PETROBRAS PN N2",
 *     "longName": "Petróleo Brasileiro S.A. - Petrobras",
 *     "currency": "BRL",
 *     "regularMarketPrice": 38.15,
 *     "regularMarketDayHigh": 38.80,
 *     "regularMarketDayLow": 37.50,
 *     "regularMarketVolume": 42000000,
 *     "regularMarketChange": 0.35,
 *     "regularMarketChangePercent": 0.926,
 *     "marketCap": 500000000000
 *   }],
 *   "requestedAt": "2026-02-19T15:00:00.000Z"
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
            BigDecimal marketCap
    ) {
    }
}
