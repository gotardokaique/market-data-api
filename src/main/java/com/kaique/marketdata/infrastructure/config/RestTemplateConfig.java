package com.kaique.marketdata.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();

        // Interceptor global que adiciona User-Agent em TODAS as requisições.
        // A API do Yahoo Finance retorna 429 se o User-Agent estiver ausente ou for genérico.
        restTemplate.setInterceptors(List.of(new UserAgentInterceptor()));

        return restTemplate;
    }

    private static class UserAgentInterceptor implements ClientHttpRequestInterceptor {

        private static final String USER_AGENT =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set("User-Agent", USER_AGENT);
            return execution.execute(request, body);
        }
    }
}
