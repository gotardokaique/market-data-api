package com.kaique.marketdata.application.service;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.TimeRange;
import com.kaique.marketdata.domain.exception.ProviderException;
import com.kaique.marketdata.domain.model.Candle;
import com.kaique.marketdata.domain.model.MarketData;
import com.kaique.marketdata.infrastructure.metrics.ProviderMetrics;
import com.kaique.marketdata.infrastructure.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestrador de dados de mercado.
 * Implementa Strategy Pattern (MarketDataProvider) com Fallback e Métricas.
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

    public MarketData getCurrentPrice(MarketType marketType, String symbol) {
        log.info("Requisição recebida: type={}, symbol={}", marketType, symbol);

        List<MarketDataProvider> supportedProviders = getSupportedProviders(marketType);
        ProviderException lastException = null;

        for (MarketDataProvider provider : supportedProviders) {
            String providerName = provider.getClass().getSimpleName();

            try {
                log.info("Tentando provider: {}", providerName);

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

    public List<Candle> getHistory(MarketType marketType, String symbol, TimeRange timeRange) {
        log.info("Requisição de histórico: type={}, symbol={}, range={}", marketType, symbol, timeRange);

        List<MarketDataProvider> supportedProviders = getSupportedProviders(marketType);
        ProviderException lastException = null;

        for (MarketDataProvider provider : supportedProviders) {
            String providerName = provider.getClass().getSimpleName();

            try {
                log.info("Tentando histórico via provider: {} (range={})", providerName, timeRange);

                List<Candle> candles = metrics.recordLatency(providerName, symbol,
                        () -> provider.fetchHistory(symbol, timeRange));

                log.info("Sucesso: {} retornou {} candles para {} (range={})",
                        providerName, candles.size(), symbol, timeRange);
                return candles;

            } catch (ProviderException e) {
                log.warn("Provider {} falhou ao buscar histórico de {}: {}. Tentando fallback...",
                        providerName, symbol, e.getMessage());
                lastException = e;
            }
        }

        log.error("Todos os {} provider(s) falharam ao buscar histórico para type={}, symbol={}",
                supportedProviders.size(), marketType, symbol);
        throw lastException;
    }

    private List<MarketDataProvider> getSupportedProviders(MarketType marketType) {
        List<MarketDataProvider> supported = providers.stream()
                .filter(p -> p.supports(marketType))
                .toList();

        if (supported.isEmpty()) {
            log.error("Nenhum provider encontrado para o tipo: {}", marketType);
            throw new UnsupportedOperationException(
                    "Nenhum provider disponível para o tipo de mercado: " + marketType);
        }

        return supported;
    }
}
