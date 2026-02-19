package com.kaique.marketdata.infrastructure.provider.coingecko;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.ProviderType;
import com.kaique.marketdata.domain.enums.TimeRange;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.Candle;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Provider para a API CoinGecko.
 * Endpoint: /coins/{id}
 */
@Component
public class CoinGeckoProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoProvider.class);
    private static final String BASE_URL = "https://api.coingecko.com/api/v3";
    private static final String PROVIDER_NAME = "CoinGecko";

    private final RestTemplate restTemplate;

    public CoinGeckoProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public MarketData fetchCurrentPrice(String symbol) {
        String url = BASE_URL + "/coins/" + symbol.toLowerCase()
                + "?localization=false&tickers=false&community_data=false&developer_data=false&sparkline=false";

        log.info("[{}] Buscando preço para símbolo: {}", PROVIDER_NAME, symbol);

        try {
            CoinGeckoDTO response = restTemplate.getForObject(url, CoinGeckoDTO.class);

            if (response == null || response.getMarketData() == null) {
                throw new ProviderException(PROVIDER_NAME,
                        "Resposta nula ou sem market_data para o símbolo: " + symbol);
            }

            return mapToMarketData(response);

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

    @Override
    public List<Candle> fetchHistory(String symbol, TimeRange timeRange) {
        String coinId = symbol.toLowerCase();
        String url = BASE_URL + "/coins/" + coinId
                + "/ohlc?vs_currency=usd&days=" + timeRange.getCoinGeckoDays();

        log.info("[{}] Buscando histórico OHLC para {} (days={})",
                PROVIDER_NAME, coinId, timeRange.getCoinGeckoDays());

        try {
            // CoinGecko /ohlc retorna List<List<Number>>: [[ts_ms, open, high, low, close], ...]
            ResponseEntity<List<List<Number>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<List<Number>> rawData = response.getBody();

            if (rawData == null || rawData.isEmpty()) {
                log.warn("[{}] Nenhum dado OHLC retornado para {}", PROVIDER_NAME, coinId);
                return Collections.emptyList();
            }

            List<Candle> candles = rawData.stream()
                    .filter(point -> point != null && point.size() >= 5)
                    .map(this::mapOhlcToCandle)
                    .sorted((a, b) -> Long.compare(a.timestamp(), b.timestamp()))
                    .toList();

            log.info("[{}] Retornados {} candles para {} (days={})",
                    PROVIDER_NAME, candles.size(), coinId, timeRange.getCoinGeckoDays());

            return candles;

        } catch (ProviderException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("[{}] Erro 4xx ao buscar histórico de {}: {}", PROVIDER_NAME, coinId, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do cliente ao buscar histórico de " + coinId + ": " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("[{}] Erro 5xx ao buscar histórico de {}: {}", PROVIDER_NAME, coinId, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Erro do servidor ao buscar histórico de " + coinId + ": " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("[{}] Erro de conexão ao buscar histórico de {}: {}", PROVIDER_NAME, coinId, e.getMessage());
            throw new ProviderException(PROVIDER_NAME,
                    "Falha na conexão ao buscar histórico de " + coinId, e);
        }
    }

    @Override
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.CRYPTO;
    }

    // ========== Métodos de mapeamento ==========

    /**
     * Converte [timestamp_ms, open, high, low, close] para Candle (seconds, volume=0).
     */
    private Candle mapOhlcToCandle(List<Number> point) {
        // point[0] = timestamp em milissegundos → converter para segundos
        long timestampSeconds = point.get(0).longValue() / 1000;

        return new Candle(
                timestampSeconds,
                toBigDecimal(point.get(1)),
                toBigDecimal(point.get(2)),
                toBigDecimal(point.get(3)),
                toBigDecimal(point.get(4)),
                BigDecimal.ZERO // CoinGecko /ohlc não inclui volume
        );
    }

    private BigDecimal toBigDecimal(Number value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private MarketData mapToMarketData(CoinGeckoDTO dto) {
        CoinGeckoDTO.MarketDataDTO data = dto.getMarketData();

        return new MarketData(
                dto.getSymbol().toUpperCase(),
                dto.getName(),
                data.getCurrentPrice() != null ? data.getCurrentPrice().getUsd() : BigDecimal.ZERO,
                "USD",
                data.getPriceChangePercentage24h() != null ? data.getPriceChangePercentage24h() : BigDecimal.ZERO,
                data.getMarketCap() != null ? data.getMarketCap().getUsd() : BigDecimal.ZERO,
                data.getTotalVolume() != null ? data.getTotalVolume().getUsd() : BigDecimal.ZERO,
                MarketType.CRYPTO,
                ProviderType.COINGECKO,
                Instant.now()
        );
    }
}
