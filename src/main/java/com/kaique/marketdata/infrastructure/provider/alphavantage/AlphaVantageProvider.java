package com.kaique.marketdata.infrastructure.provider.alphavantage;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.ProviderType;
import com.kaique.marketdata.domain.enums.TimeRange;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.Candle;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Provider para Alpha Vantage (Ações Globais).
 * Endpoints: GLOBAL_QUOTE e TIME_SERIES_DAILY.
 * @Order(2) — secundário à Brapi.
 */
@Component
@Order(2)
public class AlphaVantageProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageProvider.class);
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String PROVIDER_NAME = "AlphaVantage";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final String apiKey;

    public AlphaVantageProvider(RestTemplate restTemplate,
                                @Value("${alphavantage.apikey}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    @Override
    public MarketData fetchCurrentPrice(String symbol) {
        String avSymbol = convertSymbol(symbol);
        String url = BASE_URL + "?function=GLOBAL_QUOTE&symbol=" + avSymbol + "&apikey=" + apiKey;

        log.info("[{}] Buscando preço para símbolo: {} (AV: {})", PROVIDER_NAME, symbol, avSymbol);

        try {
            AlphaVantageResponse response = restTemplate.getForObject(
                    URI.create(url), AlphaVantageResponse.class);

            if (response == null || response.globalQuote() == null) {
                throw new ProviderException(PROVIDER_NAME,
                        "Resposta nula para o símbolo: " + avSymbol);
            }

            AlphaVantageResponse.GlobalQuote quote = response.globalQuote();

            if (quote.symbol() == null || quote.price() == null) {
                throw new ProviderException(PROVIDER_NAME,
                        "Símbolo não encontrado ou sem dados: " + avSymbol);
            }

            return mapToMarketData(quote, symbol);

        } catch (ProviderException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("[{}] Erro 4xx ao buscar {}: {} - {}", PROVIDER_NAME, avSymbol, e.getStatusCode(), e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do cliente ao buscar " + avSymbol + ": " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("[{}] Erro 5xx ao buscar {}: {} - {}", PROVIDER_NAME, avSymbol, e.getStatusCode(), e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do servidor ao buscar " + avSymbol + ": " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("[{}] Erro de conexão ao buscar {}: {}", PROVIDER_NAME, avSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Falha na conexão ao buscar " + avSymbol, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Candle> fetchHistory(String symbol, TimeRange timeRange) {
        String avSymbol = convertSymbol(symbol);

        // Para ranges curtos (1D, 1W) usa compact (últimos 100 pontos),
        // para maiores usa full (20+ anos)
        String outputSize = (timeRange == TimeRange.ONE_DAY || timeRange == TimeRange.ONE_WEEK
                || timeRange == TimeRange.ONE_MONTH) ? "compact" : "full";

        String url = BASE_URL + "?function=TIME_SERIES_DAILY&symbol=" + avSymbol
                + "&outputsize=" + outputSize + "&apikey=" + apiKey;

        log.info("[{}] Buscando histórico para {} (outputsize={})", PROVIDER_NAME, avSymbol, outputSize);

        try {
            // Alpha Vantage retorna JSON dinâmico com chaves de data, precisa de Map genérico
            Map<String, Object> rawResponse = restTemplate.getForObject(URI.create(url), Map.class);

            if (rawResponse == null) {
                throw new ProviderException(PROVIDER_NAME,
                        "Resposta nula ao buscar histórico de " + avSymbol);
            }

            // Checa se Alpha Vantage retornou mensagem de erro/info
            if (rawResponse.containsKey("Information") || rawResponse.containsKey("Error Message")) {
                String msg = rawResponse.getOrDefault("Information",
                        rawResponse.get("Error Message")).toString();
                throw new ProviderException(PROVIDER_NAME,
                        "Alpha Vantage retornou: " + msg);
            }

            Map<String, Map<String, String>> timeSeries =
                    (Map<String, Map<String, String>>) rawResponse.get("Time Series (Daily)");

            if (timeSeries == null || timeSeries.isEmpty()) {
                log.warn("[{}] Nenhum dado histórico retornado para {}", PROVIDER_NAME, avSymbol);
                return Collections.emptyList();
            }

            // Calcular a data de corte baseada no TimeRange
            LocalDate cutoffDate = calculateCutoffDate(timeRange);

            List<Candle> candles = timeSeries.entrySet().stream()
                    .filter(entry -> {
                        LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMAT);
                        return !date.isBefore(cutoffDate);
                    })
                    .map(entry -> mapTimeSeriesEntryToCandle(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparingLong(Candle::timestamp))
                    .toList();

            log.info("[{}] Retornados {} candles para {} (cutoff={})",
                    PROVIDER_NAME, candles.size(), avSymbol, cutoffDate);

            return candles;

        } catch (ProviderException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("[{}] Erro 4xx ao buscar histórico de {}: {}", PROVIDER_NAME, avSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do cliente ao buscar histórico de " + avSymbol + ": " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("[{}] Erro 5xx ao buscar histórico de {}: {}", PROVIDER_NAME, avSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do servidor ao buscar histórico de " + avSymbol + ": " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("[{}] Erro de conexão ao buscar histórico de {}: {}", PROVIDER_NAME, avSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Falha na conexão ao buscar histórico de " + avSymbol, e);
        }
    }

    @Override
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.STOCK;
    }

    // ========== Métodos privados ==========

    private String convertSymbol(String symbol) {
        String upper = symbol.toUpperCase();
        if (upper.endsWith(".SA")) {
            return upper.replace(".SA", ".SAO");
        }
        return upper;
    }

    /** Calcula data de corte para filtrar response full da Alpha Vantage. */
    private LocalDate calculateCutoffDate(TimeRange timeRange) {
        LocalDate today = LocalDate.now();
        return switch (timeRange) {
            case ONE_DAY -> today.minusDays(1);
            case ONE_WEEK -> today.minusWeeks(1);
            case ONE_MONTH -> today.minusMonths(1);
            case THREE_MONTHS -> today.minusMonths(3);
            case SIX_MONTHS -> today.minusMonths(6);
            case ONE_YEAR -> today.minusYears(1);
            case FIVE_YEARS -> today.minusYears(5);
        };
    }

    /** Converte entry "yyyy-MM-dd" → Candle (epoch seconds). */
    private Candle mapTimeSeriesEntryToCandle(String dateStr, Map<String, String> values) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
        long epochSeconds = date.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        return new Candle(
                epochSeconds,
                parseBigDecimal(values.get("1. open")),
                parseBigDecimal(values.get("2. high")),
                parseBigDecimal(values.get("3. low")),
                parseBigDecimal(values.get("4. close")),
                parseBigDecimal(values.get("5. volume"))
        );
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private MarketData mapToMarketData(AlphaVantageResponse.GlobalQuote quote, String originalSymbol) {
        BigDecimal changePercent = parseChangePercent(quote.changePercent());
        String currency = originalSymbol.toUpperCase().contains(".SA") ? "BRL" : "USD";

        return new MarketData(
                quote.symbol() != null ? quote.symbol() : originalSymbol.toUpperCase(),
                originalSymbol.toUpperCase(),
                quote.price() != null ? quote.price() : BigDecimal.ZERO,
                currency,
                changePercent,
                BigDecimal.ZERO,
                quote.volume() != null ? quote.volume() : BigDecimal.ZERO,
                MarketType.STOCK,
                ProviderType.ALPHA_VANTAGE,
                Instant.now()
        );
    }

    private BigDecimal parseChangePercent(String changePercentStr) {
        if (changePercentStr == null || changePercentStr.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(changePercentStr.replace("%", "").trim());
        } catch (NumberFormatException e) {
            log.warn("[{}] Falha ao parsear changePercent: {}", PROVIDER_NAME, changePercentStr);
            return BigDecimal.ZERO;
        }
    }
}
