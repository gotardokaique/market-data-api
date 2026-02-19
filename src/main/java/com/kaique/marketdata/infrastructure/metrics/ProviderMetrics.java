package com.kaique.marketdata.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Componente responsável por registrar métricas de latência e erros dos providers.
 *
 * Métricas registradas:
 *   - market.provider.latency (Timer): tempo de resposta por provider e símbolo
 *   - market.provider.errors (Counter): contagem de erros por provider
 *
 * Consulta via Actuator:
 *   GET /actuator/metrics/market.provider.latency
 *   GET /actuator/metrics/market.provider.errors
 *
 * Exemplos de filtros:
 *   GET /actuator/metrics/market.provider.latency?tag=provider:CoinGeckoProvider
 *   GET /actuator/metrics/market.provider.latency?tag=provider:BrapiProvider
 */
@Component
public class ProviderMetrics {

    private final MeterRegistry registry;

    public ProviderMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executa uma operação de provider medindo o tempo de execução.
     * Em caso de erro, incrementa o counter de erros antes de relançar a exceção.
     *
     * @param providerName nome do provider (ex: "CoinGeckoProvider", "BrapiProvider")
     * @param symbol       símbolo consultado
     * @param operation    a operação a ser executada (fetchCurrentPrice)
     * @param <T>          tipo de retorno
     * @return resultado da operação
     */
    public <T> T recordLatency(String providerName, String symbol, Supplier<T> operation) {
        Timer.Sample sample = Timer.start(registry);

        try {
            T result = operation.get();

            sample.stop(Timer.builder("market.provider.latency")
                    .tag("provider", providerName)
                    .tag("symbol", symbol)
                    .tag("status", "success")
                    .description("Latência das chamadas aos providers de dados de mercado")
                    .register(registry));

            return result;

        } catch (Exception e) {
            sample.stop(Timer.builder("market.provider.latency")
                    .tag("provider", providerName)
                    .tag("symbol", symbol)
                    .tag("status", "error")
                    .description("Latência das chamadas aos providers de dados de mercado")
                    .register(registry));

            Counter.builder("market.provider.errors")
                    .tag("provider", providerName)
                    .tag("symbol", symbol)
                    .description("Contagem de erros por provider")
                    .register(registry)
                    .increment();

            throw e;
        }
    }
}
