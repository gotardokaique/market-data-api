package com.kaique.marketdata.application.service;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.metrics.ProviderMetrics;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço orquestrador de dados de mercado.
 *
 * Strategy Pattern com Fallback e Métricas:
 * - Itera por todos os providers que suportam o MarketType (ordenados por @Order).
 * - Cada chamada é instrumentada com Micrometer (latência + contagem de erros).
 * - Se o primeiro provider falhar, tenta o próximo (fallback).
 *
 * Métricas disponíveis em /actuator/metrics:
 *   - market.provider.latency → Timer por provider/símbolo/status
 *   - market.provider.errors  → Counter de erros por provider/símbolo
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final List<MarketDataProvider> providers;
    private final ProviderMetrics metrics;

    public MarketDataService(List<MarketDataProvider> providers, ProviderMetrics metrics) {
        this.providers = providers;
        this.metrics = metrics;
        log.info("MarketDataService inicializado com {} provider(s): {}",
                providers.size(),
                providers.stream()
                        .map(p -> p.getClass().getSimpleName())
                        .toList());
    }

    /**
     * Busca o preço atual de um ativo, delegando para o provider adequado.
     * Cada chamada é medida individualmente para monitoramento de latência.
     *
     * @param marketType tipo de mercado (CRYPTO, STOCK, FII)
     * @param symbol     símbolo do ativo (ex: "bitcoin", "PETR4.SA")
     * @return MarketData com as informações do preço atual
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
            String providerName = provider.getClass().getSimpleName();

            try {
                log.info("Tentando provider: {}", providerName);

                // Cada chamada é instrumentada — latência e status ficam registrados
                MarketData result = metrics.recordLatency(providerName, symbol,
                        () -> provider.fetchCurrentPrice(symbol));

                log.info("Sucesso com provider: {} (symbol={})", providerName, symbol);
                return result;

            } catch (ProviderException e) {
                log.warn("Provider {} falhou para {}: {}. Tentando fallback...",
                        providerName, symbol, e.getMessage());
                lastException = e;
            }
        }

        log.error("Todos os {} provider(s) falharam para type={}, symbol={}",
                supportedProviders.size(), marketType, symbol);
        throw lastException;
    }
}
