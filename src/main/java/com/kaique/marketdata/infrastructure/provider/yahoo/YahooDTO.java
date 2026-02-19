package com.kaique.marketdata.infrastructure.provider.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO que mapeia a resposta da API Yahoo Finance (endpoint /v8/finance/chart/{symbol}).
 *
 * Exemplo de resposta parcial:
 * {
 *   "chart": {
 *     "result": [{
 *       "meta": {
 *         "symbol": "PETR4.SA",
 *         "shortName": "PETROBRAS PN",
 *         "regularMarketPrice": 38.15,
 *         "currency": "BRL",
 *         "regularMarketVolume": 42000000,
 *         "previousClose": 37.80
 *       }
 *     }],
 *     "error": null
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooDTO {

    private ChartDTO chart;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChartDTO {

        private List<ResultDTO> result;
        private Object error;

        public List<ResultDTO> getResult() {
            return result;
        }

        public void setResult(List<ResultDTO> result) {
            this.result = result;
        }

        public Object getError() {
            return error;
        }

        public void setError(Object error) {
            this.error = error;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultDTO {

        private MetaDTO meta;

        public MetaDTO getMeta() {
            return meta;
        }

        public void setMeta(MetaDTO meta) {
            this.meta = meta;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaDTO {

        private String symbol;

        @JsonProperty("shortName")
        private String shortName;

        @JsonProperty("regularMarketPrice")
        private BigDecimal regularMarketPrice;

        private String currency;

        @JsonProperty("regularMarketVolume")
        private BigDecimal regularMarketVolume;

        @JsonProperty("previousClose")
        private BigDecimal previousClose;

        @JsonProperty("marketCap")
        private BigDecimal marketCap;

        // Getters e Setters

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public BigDecimal getRegularMarketPrice() {
            return regularMarketPrice;
        }

        public void setRegularMarketPrice(BigDecimal regularMarketPrice) {
            this.regularMarketPrice = regularMarketPrice;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getRegularMarketVolume() {
            return regularMarketVolume;
        }

        public void setRegularMarketVolume(BigDecimal regularMarketVolume) {
            this.regularMarketVolume = regularMarketVolume;
        }

        public BigDecimal getPreviousClose() {
            return previousClose;
        }

        public void setPreviousClose(BigDecimal previousClose) {
            this.previousClose = previousClose;
        }

        public BigDecimal getMarketCap() {
            return marketCap;
        }

        public void setMarketCap(BigDecimal marketCap) {
            this.marketCap = marketCap;
        }
    }

    // Getters e Setters

    public ChartDTO getChart() {
        return chart;
    }

    public void setChart(ChartDTO chart) {
        this.chart = chart;
    }
}
