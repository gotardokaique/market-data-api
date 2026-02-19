package com.kaique.marketdata.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuração do RestTemplate com timeouts estritos.
 *
 * REGRA: Nunca faça uma chamada HTTP externa sem timeout definido.
 * Se um provider externo demorar mais que o limite, a requisição é cancelada
 * para não travar threads do Tomcat.
 *
 * Os valores são configuráveis via application.properties:
 * - provider.timeout.connect-ms (default: 2000ms)
 * - provider.timeout.read-ms (default: 2000ms)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${provider.timeout.connect-ms:2000}") int connectTimeoutMs,
            @Value("${provider.timeout.read-ms:2000}") int readTimeoutMs) {

        return builder
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
