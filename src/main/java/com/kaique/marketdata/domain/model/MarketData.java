package com.kaique.marketdata.domain.model;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.ProviderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Objeto de domínio que representa os dados de mercado de qualquer ativo.
 * Este é o contrato único que toda a aplicação utiliza — nenhum DTO externo vaza para fora da camada de infraestrutura.
 */
public record MarketData(
        String symbol,
        String name,
        BigDecimal currentPrice,
        String currency,
        BigDecimal changePercent24h,
        BigDecimal marketCap,
        BigDecimal volume24h,
        MarketType marketType,
        ProviderType providerType,
        Instant timestamp
) {
}
