package com.kaique.marketdata.infrastructure.provider.brapi;

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
import java.util.Collections;
import java.util.List;

/**
 * Provider para a API Brapi (https://brapi.dev).
 * Endpoint: https://brapi.dev/api/quote/{symbol}
 *
 * @Order(1) garante prioridade sobre Alpha Vantage.
 */
@Component
@Order(1)
public class BrapiProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(BrapiProvider.class);
    private static final String BASE_URL = "https://brapi.dev/api/quote/";
    private static final String PROVIDER_NAME = "Brapi";

    private final RestTemplate restTemplate;
    private final String token;

    public BrapiProvider(RestTemplate restTemplate,
                         @Value("${brapi.token}") String token) {
        this.restTemplate = restTemplate;
        this.token = token.trim();
    }

    @Override
    public MarketData fetchCurrentPrice(String symbol) {
        String cleanSymbol = cleanSymbol(symbol);
        String url = BASE_URL + cleanSymbol + "?token=" + token;

        log.info("[{}] Buscando preço para símbolo: {}", PROVIDER_NAME, cleanSymbol);

        try {
            BrapiResponse response = restTemplate.getForObject(URI.create(url), BrapiResponse.class);
            validateResponse(response, cleanSymbol);

            BrapiResponse.BrapiQuote quote = response.results().get(0);
            return mapToMarketData(quote, cleanSymbol);

        } catch (ProviderException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("[{}] Erro 4xx ao buscar {}: {} - {}", PROVIDER_NAME, cleanSymbol, e.getStatusCode(), e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do cliente ao buscar " + cleanSymbol + ": " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("[{}] Erro 5xx ao buscar {}: {} - {}", PROVIDER_NAME, cleanSymbol, e.getStatusCode(), e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do servidor ao buscar " + cleanSymbol + ": " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("[{}] Erro de conexão ao buscar {}: {}", PROVIDER_NAME, cleanSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Falha na conexão ao buscar " + cleanSymbol, e);
        }
    }

    @Override
    public List<Candle> fetchHistory(String symbol, TimeRange timeRange) {
        String cleanSymbol = cleanSymbol(symbol);
        String url = BASE_URL + cleanSymbol
                + "?range=" + timeRange.getBrapiRange()
                + "&interval=" + timeRange.getBrapiInterval()
                + "&token=" + token;

        log.info("[{}] Buscando histórico para {} (range={}, interval={})",
                PROVIDER_NAME, cleanSymbol, timeRange.getBrapiRange(), timeRange.getBrapiInterval());

        try {
            BrapiResponse response = restTemplate.getForObject(URI.create(url), BrapiResponse.class);
            validateResponse(response, cleanSymbol);

            BrapiResponse.BrapiQuote quote = response.results().get(0);

            if (quote.historicalDataPrice() == null || quote.historicalDataPrice().isEmpty()) {
                log.warn("[{}] Nenhum dado histórico retornado para {}", PROVIDER_NAME, cleanSymbol);
                return Collections.emptyList();
            }

            List<Candle> candles = quote.historicalDataPrice().stream()
                    .map(this::mapToCandle)
                    .sorted((a, b) -> Long.compare(a.timestamp(), b.timestamp()))
                    .toList();

            log.info("[{}] Retornados {} candles para {} (range={})",
                    PROVIDER_NAME, candles.size(), cleanSymbol, timeRange.getBrapiRange());

            return candles;

        } catch (ProviderException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("[{}] Erro 4xx ao buscar histórico de {}: {}", PROVIDER_NAME, cleanSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do cliente ao buscar histórico de " + cleanSymbol + ": " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("[{}] Erro 5xx ao buscar histórico de {}: {}", PROVIDER_NAME, cleanSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do servidor ao buscar histórico de " + cleanSymbol + ": " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("[{}] Erro de conexão ao buscar histórico de {}: {}", PROVIDER_NAME, cleanSymbol, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Falha na conexão ao buscar histórico de " + cleanSymbol, e);
        }
    }

    @Override
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.STOCK || marketType == MarketType.FII;
    }

    // ========== Métodos privados ==========

    private String cleanSymbol(String symbol) {
        return symbol.toUpperCase().replace(".SA", "");
    }

    private void validateResponse(BrapiResponse response, String symbol) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new ProviderException(PROVIDER_NAME,
                    "Resposta nula ou sem resultados para o símbolo: " + symbol);
        }
    }

    /**
     * Converte historico Brapi (date=epoch seconds) para Candle.
     */
    private Candle mapToCandle(BrapiResponse.HistoricalDataPrice data) {
        return new Candle(
                data.date(),
                data.open() != null ? data.open() : BigDecimal.ZERO,
                data.high() != null ? data.high() : BigDecimal.ZERO,
                data.low() != null ? data.low() : BigDecimal.ZERO,
                data.close() != null ? data.close() : BigDecimal.ZERO,
                data.volume() != null ? data.volume() : BigDecimal.ZERO
        );
    }

    private MarketData mapToMarketData(BrapiResponse.BrapiQuote quote, String originalSymbol) {
        MarketType detectedType = detectMarketType(originalSymbol);

        return new MarketData(
                quote.symbol() != null ? quote.symbol() : originalSymbol,
                quote.shortName() != null ? quote.shortName() : quote.longName(),
                quote.regularMarketPrice() != null ? quote.regularMarketPrice() : BigDecimal.ZERO,
                quote.currency() != null ? quote.currency() : "BRL",
                quote.regularMarketChangePercent() != null ? quote.regularMarketChangePercent() : BigDecimal.ZERO,
                quote.marketCap() != null ? quote.marketCap() : BigDecimal.ZERO,
                quote.regularMarketVolume() != null ? quote.regularMarketVolume() : BigDecimal.ZERO,
                detectedType,
                ProviderType.BRAPI,
                Instant.now()
        );
    }

    private MarketType detectMarketType(String symbol) {
        String upper = symbol.toUpperCase().replace(".SA", "");
        if (upper.matches("^[A-Z]{4}11$")) {
            return MarketType.FII;
        }
        return MarketType.STOCK;
    }
}
