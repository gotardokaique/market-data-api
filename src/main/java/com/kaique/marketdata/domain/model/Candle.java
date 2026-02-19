package com.kaique.marketdata.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representa um candle OHLCV (Open, High, Low, Close, Volume) para dados históricos.
 */
public record Candle(
        Instant timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {
}
