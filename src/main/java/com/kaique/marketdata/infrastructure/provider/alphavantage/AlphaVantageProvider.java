package com.kaique.marketdata.infrastructure.provider.alphavantage;

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
 * Implementação do provider para a Alpha Vantage API.
 * Responsável por buscar dados de ações globais (STOCK) — NYSE, NASDAQ, LSE, etc.
 *
 * Endpoint: GET https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={key}
 *
 * Complementa a Brapi:
 * - Brapi (@Order 1): especializada em B3 (ações e FIIs brasileiros)
 * - Alpha Vantage (@Order 2): ações globais (IBM, AAPL, MSFT, TSLA)
 *
 * Para ativos brasileiros, a Brapi será chamada primeiro (prioridade).
 * Se a Brapi falhar, o Alpha Vantage tenta como fallback (usa sufixo .SAO para B3).
 *
 * Limitações do free tier:
 * - 25 requests/dia
 * - 5 requests/minuto
 *
 * @see <a href="https://www.alphavantage.co/documentation/#latestprice">Alpha Vantage GLOBAL_QUOTE Docs</a>
 */
@Component
@Order(2)
public class AlphaVantageProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageProvider.class);
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String PROVIDER_NAME = "AlphaVantage";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public AlphaVantageProvider(RestTemplate restTemplate,
                                @Value("${alphavantage.apikey}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    @Override
    public MarketData fetchCurrentPrice(String symbol) {
        // Converte tickers brasileiros: .SA → .SAO (formato Alpha Vantage)
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

            // Alpha Vantage retorna objeto vazio se o símbolo não existir
            if (quote.symbol() == null || quote.price() == null) {
                throw new ProviderException(PROVIDER_NAME,
                        "Símbolo não encontrado ou sem dados: " + avSymbol);
            }

            return mapToMarketData(quote, symbol);

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
    public boolean supports(MarketType marketType) {
        return marketType == MarketType.STOCK;
    }

    /**
     * Converte o formato do ticker para o padrão Alpha Vantage.
     * Tickers brasileiros usam ".SAO" na Alpha Vantage (ex: PETR4.SAO), não ".SA".
     */
    private String convertSymbol(String symbol) {
        String upper = symbol.toUpperCase();
        if (upper.endsWith(".SA")) {
            return upper.replace(".SA", ".SAO");
        }
        return upper;
    }

    /**
     * Converte o GlobalQuote da Alpha Vantage para o objeto de domínio MarketData.
     */
    private MarketData mapToMarketData(AlphaVantageResponse.GlobalQuote quote, String originalSymbol) {
        BigDecimal changePercent = parseChangePercent(quote.changePercent());

        // Inferir moeda: se é ativo brasileiro (.SA/.SAO), assume BRL; senão, USD
        String currency = originalSymbol.toUpperCase().contains(".SA") ? "BRL" : "USD";

        return new MarketData(
                quote.symbol() != null ? quote.symbol() : originalSymbol.toUpperCase(),
                originalSymbol.toUpperCase(), // Alpha Vantage não retorna nome do ativo no GLOBAL_QUOTE
                quote.price() != null ? quote.price() : BigDecimal.ZERO,
                currency,
                changePercent,
                BigDecimal.ZERO, // GLOBAL_QUOTE não retorna marketCap
                quote.volume() != null ? quote.volume() : BigDecimal.ZERO,
                MarketType.STOCK,
                ProviderType.ALPHA_VANTAGE,
                Instant.now()
        );
    }

    /**
     * Faz o parse do changePercent que vem como String com "%" (ex: "0.9601%").
     */
    private BigDecimal parseChangePercent(String changePercentStr) {
        if (changePercentStr == null || changePercentStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            String cleaned = changePercentStr.replace("%", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("[{}] Falha ao parsear changePercent: {}", PROVIDER_NAME, changePercentStr);
            return BigDecimal.ZERO;
        }
    }
}
