package com.kaique.marketdata.presentation.handler;

import com.kaique.marketdata.domain.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Handler global de exceções para a API.
 * Garante que erros internos não vazem detalhes de implementação para o cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<Map<String, Object>> handleProviderException(ProviderException ex) {
        log.error("Erro no provider {}: {}", ex.getProviderName(), ex.getMessage(), ex);

        Map<String, Object> body = Map.of(
                "error", "Falha ao consultar provedor de dados",
                "provider", ex.getProviderName(),
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());

        Map<String, Object> body = Map.of(
                "error", "Parâmetro inválido",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupported(UnsupportedOperationException ex) {
        log.warn("Operação não suportada: {}", ex.getMessage());

        Map<String, Object> body = Map.of(
                "error", "Tipo de mercado não suportado",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);

        Map<String, Object> body = Map.of(
                "error", "Erro interno do servidor",
                "message", "Ocorreu um erro inesperado. Tente novamente.",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
