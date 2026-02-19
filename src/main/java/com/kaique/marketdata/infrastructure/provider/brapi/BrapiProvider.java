package com.kaique.marketdata.infrastructure.provider.brapi;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.ProviderType;
import com.kaique.marketdata.domain.exception.ProviderException;
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

/**
 * Implementação do provider para a API Brapi (https://brapi.dev).
 * Responsável por buscar dados de ações (STOCK) e fundos imobiliários (FII) da B3.
 *
 * Endpoint: GET https://brapi.dev/api/quote/{symbol}?token={token}
 *
 * A Brapi tem PRIORIDADE sobre o Yahoo Finance para ativos brasileiros porque:
 * - JSON limpo e estruturado
 * - Sem necessidade de User-Agent ou interceptores
 * - Rate limiting razoável com token
 *
 * A anotação @Order(1) garante que este provider será selecionado ANTES do
 * YahooFinanceProvider (@Order(2)) quando ambos suportam o mesmo MarketType.
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
        this.token = token;
    }

    @Override
    public MarketData fetchCurrentPrice(String symbol) {
        // Remove o ".SA" se o usuário enviar — a Brapi usa ticker sem sufixo
        String cleanSymbol = symbol.toUpperCase().replace(".SA", "");
        String url = BASE_URL + cleanSymbol + "?token=" + token;

        log.info("[{}] Buscando preço para símbolo: {}", PROVIDER_NAME, cleanSymbol);

        try {
            BrapiResponse response = restTemplate.getForObject(URI.create(url), BrapiResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new ProviderException(PROVIDER_NAME,
                        "Resposta nula ou sem resultados para o símbolo: " + cleanSymbol);
            }

            BrapiResponse.BrapiQuote quote = response.results().get(0);
            return mapToMarketData(quote, cleanSymbol);

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
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.STOCK || marketType == MarketType.FII;
    }

    /**
     * Converte o quote da Brapi para o objeto de domínio MarketData.
     */
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

    /**
     * Detecta se um símbolo brasileiro é FII ou STOCK.
     * FIIs da B3 terminam com "11" (ex: HGLG11, MXRF11, XPML11).
     */
    private MarketType detectMarketType(String symbol) {
        String upper = symbol.toUpperCase().replace(".SA", "");
        if (upper.matches("^[A-Z]{4}11$")) {
            return MarketType.FII;
        }
        return MarketType.STOCK;
    }
}
