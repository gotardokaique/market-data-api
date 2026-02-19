package com.kaique.marketdata.application.service;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço orquestrador de dados de mercado.
 *
 * Este é o coração do Strategy Pattern com Fallback:
 * - Recebe uma lista de todos os MarketDataProvider registrados via injeção de dependência.
 * - Os providers são ordenados pela anotação @Order (Brapi=1, Yahoo=2).
 * - Para cada requisição, TENTA o primeiro provider que suporta o MarketType.
 * - Se o primeiro falhar (ProviderException), tenta o próximo da lista (fallback).
 * - Nunca conhece detalhes de nenhuma API externa.
 *
 * Fluxo para STOCK/FII:
 *   1. Tenta BrapiProvider (@Order 1) → se sucesso, retorna.
 *   2. Se Brapi falhar → tenta YahooFinanceProvider (@Order 2) como fallback.
 *   3. Se todos falharem → lança exceção.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final List<MarketDataProvider> providers;

    public MarketDataService(List<MarketDataProvider> providers) {
        this.providers = providers;
        log.info("MarketDataService inicializado com {} provider(s): {}",
                providers.size(),
                providers.stream()
                        .map(p -> p.getClass().getSimpleName())
                        .toList());
    }

    /**
     * Busca o preço atual de um ativo, delegando para o provider adequado.
     * Se o provider primário falhar, tenta o próximo que suporta o mesmo MarketType (fallback).
     *
     * @param marketType tipo de mercado (CRYPTO, STOCK, FII)
     * @param symbol     símbolo do ativo (ex: "bitcoin", "PETR4.SA")
     * @return MarketData com as informações do preço atual
     * @throws UnsupportedOperationException se nenhum provider suportar o MarketType informado
     * @throws ProviderException             se todos os providers falharem
     */
    public MarketData getCurrentPrice(MarketType marketType, String symbol) {
        log.info("Requisição recebida: type={}, symbol={}", marketType, symbol);

        List<MarketDataProvider> supportedProviders = providers.stream()
                .filter(p -> p.supports(marketType))
                .toList();

        if (supportedProviders.isEmpty()) {
            log.error("Nenhum provider encontrado para o tipo: {}", marketType);
            throw new UnsupportedOperationException(
                    "Nenhum provider disponível para o tipo de mercado: " + marketType);
        }

        ProviderException lastException = null;

        for (MarketDataProvider provider : supportedProviders) {
            try {
                log.info("Tentando provider: {}", provider.getClass().getSimpleName());
                MarketData result = provider.fetchCurrentPrice(symbol);
                log.info("Sucesso com provider: {}", provider.getClass().getSimpleName());
                return result;

            } catch (ProviderException e) {
                log.warn("Provider {} falhou para {}: {}. Tentando fallback...",
                        provider.getClass().getSimpleName(), symbol, e.getMessage());
                lastException = e;
            }
        }

        // Todos os providers falharam
        log.error("Todos os {} provider(s) falharam para type={}, symbol={}",
                supportedProviders.size(), marketType, symbol);
        throw lastException;
    }
}
