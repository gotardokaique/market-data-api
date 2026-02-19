package com.kaique.marketdata.domain.exception;

/**
 * Exceção lançada quando ocorre um erro na comunicação com um provedor de dados externo.
 */
public class ProviderException extends RuntimeException {

    private final String providerName;

    public ProviderException(String providerName, String message) {
        super(String.format("[%s] %s", providerName, message));
        this.providerName = providerName;
    }

    public ProviderException(String providerName, String message, Throwable cause) {
        super(String.format("[%s] %s", providerName, message), cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
