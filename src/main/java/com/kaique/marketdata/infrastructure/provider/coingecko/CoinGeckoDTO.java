package com.kaique.marketdata.infrastructure.provider.coingecko;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO que mapeia a resposta da API CoinGecko (endpoint /coins/{id}).
 * Somente os campos necessários são mapeados — o resto é ignorado via @JsonIgnoreProperties.
 *
 * Exemplo de resposta parcial da CoinGecko:
 * {
 *   "id": "bitcoin",
 *   "symbol": "btc",
 *   "name": "Bitcoin",
 *   "market_data": {
 *     "current_price": { "usd": 67000.0 },
 *     "price_change_percentage_24h": 2.5,
 *     "market_cap": { "usd": 1300000000000 },
 *     "total_volume": { "usd": 35000000000 }
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinGeckoDTO {

    private String id;
    private String symbol;
    private String name;

    @JsonProperty("market_data")
    private MarketDataDTO marketData;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MarketDataDTO {

        @JsonProperty("current_price")
        private CurrencyValueDTO currentPrice;

        @JsonProperty("price_change_percentage_24h")
        private BigDecimal priceChangePercentage24h;

        @JsonProperty("market_cap")
        private CurrencyValueDTO marketCap;

        @JsonProperty("total_volume")
        private CurrencyValueDTO totalVolume;

        public CurrencyValueDTO getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(CurrencyValueDTO currentPrice) {
            this.currentPrice = currentPrice;
        }

        public BigDecimal getPriceChangePercentage24h() {
            return priceChangePercentage24h;
        }

        public void setPriceChangePercentage24h(BigDecimal priceChangePercentage24h) {
            this.priceChangePercentage24h = priceChangePercentage24h;
        }

        public CurrencyValueDTO getMarketCap() {
            return marketCap;
        }

        public void setMarketCap(CurrencyValueDTO marketCap) {
            this.marketCap = marketCap;
        }

        public CurrencyValueDTO getTotalVolume() {
            return totalVolume;
        }

        public void setTotalVolume(CurrencyValueDTO totalVolume) {
            this.totalVolume = totalVolume;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrencyValueDTO {

        private BigDecimal usd;

        public BigDecimal getUsd() {
            return usd;
        }

        public void setUsd(BigDecimal usd) {
            this.usd = usd;
        }
    }

    // Getters e Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MarketDataDTO getMarketData() {
        return marketData;
    }

    public void setMarketData(MarketDataDTO marketData) {
        this.marketData = marketData;
    }
}
