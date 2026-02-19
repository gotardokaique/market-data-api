package com.kaique.marketdata.infrastructure.provider.yahoo;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.ProviderType;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Implementação do provider para a API Yahoo Finance (FALLBACK).
 * Responsável por buscar dados de ações (STOCK) e fundos imobiliários (FII).
 *
 * Endpoint utilizado: GET https://query2.finance.yahoo.com/v8/finance/chart/{symbol}
 *
 * IMPORTANTE: Este provider é o FALLBACK. A Brapi (@Order(1)) tem prioridade para
 * ativos brasileiros. O Yahoo só será usado se a Brapi falhar.
 *
 * Inclui retry com backoff exponencial para lidar com rate limiting (429) do Yahoo.
 */
@Component
@Order(2)
public class YahooFinanceProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceProvider.class);
    private static final String BASE_URL = "https://query2.finance.yahoo.com/v8/finance/chart/";
    private static final String PROVIDER_NAME = "YahooFinance";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    private final RestTemplate restTemplate;
    private final HttpHeaders defaultHeaders;

    public YahooFinanceProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.defaultHeaders = new HttpHeaders();
        this.defaultHeaders.set("User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        this.defaultHeaders.set("Accept", "application/json");
    }

    @Override
    public MarketData fetchCurrentPrice(String symbol) {
        String url = BASE_URL + symbol.toUpperCase();

        log.info("[{}] Buscando preço para símbolo: {}", PROVIDER_NAME, symbol);

        HttpClientErrorException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpEntity<Void> requestEntity = new HttpEntity<>(defaultHeaders);
                ResponseEntity<YahooDTO> responseEntity = restTemplate.exchange(
                        URI.create(url), HttpMethod.GET, requestEntity, YahooDTO.class);

                YahooDTO response = responseEntity.getBody();

                if (response == null || response.getChart() == null) {
                    throw new ProviderException(PROVIDER_NAME,
                            "Resposta nula ou sem dados de chart para o símbolo: " + symbol);
                }

                List<YahooDTO.ResultDTO> results = response.getChart().getResult();
                if (results == null || results.isEmpty() || results.get(0).getMeta() == null) {
                    throw new ProviderException(PROVIDER_NAME,
                            "Nenhum resultado encontrado para o símbolo: " + symbol);
                }

                return mapToMarketData(results.get(0).getMeta(), symbol);

            } catch (HttpClientErrorException.TooManyRequests e) {
                lastException = e;
                long backoff = INITIAL_BACKOFF_MS * attempt;
                log.warn("[{}] Rate limited (429) na tentativa {}/{}. Aguardando {}ms antes de retry...",
                        PROVIDER_NAME, attempt, MAX_RETRIES, backoff);

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProviderException(PROVIDER_NAME,
                                "Retry interrompido ao buscar " + symbol, ie);
                    }
                }

            } catch (HttpClientErrorException e) {
                log.error("[{}] Erro 4xx ao buscar {}: {} - {}", PROVIDER_NAME, symbol, e.getStatusCode(), e.getMessage());
                throw new ProviderException(PROVIDER_NAME,
                        "Erro do cliente ao buscar " + symbol + ": " + e.getStatusCode(), e);

            } catch (HttpServerErrorException e) {
                log.error("[{}] Erro 5xx ao buscar {}: {} - {}", PROVIDER_NAME, symbol, e.getStatusCode(), e.getMessage());
                throw new ProviderException(PROVIDER_NAME,
                        "Erro do servidor ao buscar " + symbol + ": " + e.getStatusCode(), e);

            } catch (RestClientException e) {
                log.error("[{}] Erro de conexão ao buscar {}: {}", PROVIDER_NAME, symbol, e.getMessage());
                throw new ProviderException(PROVIDER_NAME,
                        "Falha na conexão ao buscar " + symbol, e);
            }
        }

        // Se chegou aqui, todas as tentativas falharam com 429
        log.error("[{}] Todas as {} tentativas falharam com 429 para {}", PROVIDER_NAME, MAX_RETRIES, symbol);
        throw new ProviderException(PROVIDER_NAME,
                "Rate limit excedido após " + MAX_RETRIES + " tentativas para " + symbol
                        + ". A API do Yahoo Finance pode estar temporariamente indisponível.", lastException);
    }

    @Override
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.STOCK || marketType == MarketType.FII;
    }

    /**
     * Converte os metadados do Yahoo Finance para o objeto de domínio MarketData.
     */
    private MarketData mapToMarketData(YahooDTO.MetaDTO meta, String originalSymbol) {
        BigDecimal currentPrice = meta.getRegularMarketPrice() != null
                ? meta.getRegularMarketPrice() : BigDecimal.ZERO;

        BigDecimal previousClose = meta.getPreviousClose() != null
                ? meta.getPreviousClose() : BigDecimal.ZERO;

        // Calcula a variação percentual baseada no previousClose
        BigDecimal changePercent = BigDecimal.ZERO;
        if (previousClose.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = currentPrice.subtract(previousClose)
                    .divide(previousClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Detecta se é FII ou STOCK baseado no padrão do ticker brasileiro
        MarketType detectedType = detectMarketType(originalSymbol);

        return new MarketData(
                meta.getSymbol() != null ? meta.getSymbol() : originalSymbol.toUpperCase(),
                meta.getShortName() != null ? meta.getShortName() : originalSymbol,
                currentPrice,
                meta.getCurrency() != null ? meta.getCurrency() : "BRL",
                changePercent,
                meta.getMarketCap() != null ? meta.getMarketCap() : BigDecimal.ZERO,
                meta.getRegularMarketVolume() != null ? meta.getRegularMarketVolume() : BigDecimal.ZERO,
                detectedType,
                ProviderType.YAHOO_FINANCE,
                Instant.now()
        );
    }

    /**
     * Detecta se um símbolo brasileiro é FII ou STOCK.
     * FIIs brasileiros geralmente terminam com "11" antes do ".SA" (ex: HGLG11.SA, MXRF11.SA).
     */
    private MarketType detectMarketType(String symbol) {
        String upper = symbol.toUpperCase().replace(".SA", "");
        if (upper.matches("^[A-Z]{4}11$")) {
            return MarketType.FII;
        }
        return MarketType.STOCK;
    }
}
