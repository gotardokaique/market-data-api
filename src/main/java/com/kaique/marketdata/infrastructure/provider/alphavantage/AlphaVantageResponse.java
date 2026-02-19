package com.kaique.marketdata.infrastructure.provider.alphavantage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Record que mapeia a resposta JSON do endpoint GLOBAL_QUOTE da Alpha Vantage.
 *
 * Exemplo de resposta:
 * {
 *   "Global Quote": {
 *     "01. symbol": "IBM",
 *     "02. open": "258.6400",
 *     "03. high": "261.1100",
 *     "04. low": "256.2500",
 *     "05. price": "260.7900",
 *     "06. volume": "3949229",
 *     "07. latest trading day": "2026-02-18",
 *     "08. previous close": "258.3100",
 *     "09. change": "2.4800",
 *     "10. change percent": "0.9601%"
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlphaVantageResponse(
        @JsonProperty("Global Quote") GlobalQuote globalQuote
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GlobalQuote(
            @JsonProperty("01. symbol") String symbol,
            @JsonProperty("02. open") BigDecimal open,
            @JsonProperty("03. high") BigDecimal high,
            @JsonProperty("04. low") BigDecimal low,
            @JsonProperty("05. price") BigDecimal price,
            @JsonProperty("06. volume") BigDecimal volume,
            @JsonProperty("07. latest trading day") String latestTradingDay,
            @JsonProperty("08. previous close") BigDecimal previousClose,
            @JsonProperty("09. change") BigDecimal change,
            @JsonProperty("10. change percent") String changePercent
    ) {
    }
}
