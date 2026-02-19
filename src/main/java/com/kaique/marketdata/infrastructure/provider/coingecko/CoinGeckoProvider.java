package com.kaique.marketdata.infrastructure.provider.coingecko;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.ProviderType;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Implementação do provider para a API CoinGecko.
 * Responsável por buscar dados de criptomoedas.
 *
 * Endpoint utilizado: GET https://api.coingecko.com/api/v3/coins/{id}
 * Documentação: https://docs.coingecko.com/reference/coins-id
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
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.CRYPTO;
    }

    /**
     * Converte o DTO específico da CoinGecko para o objeto de domínio MarketData.
     * Esta conversão garante que nenhum detalhe da API externa vaze para fora desta camada.
     */
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
