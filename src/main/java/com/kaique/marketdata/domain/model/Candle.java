package com.kaique.marketdata.domain.model;

import java.math.BigDecimal;

/**
 * Representa um candle OHLCV (Open, High, Low, Close, Volume) para dados históricos.
 *
 * O campo {@code timestamp} usa Unix Epoch em SEGUNDOS (não milissegundos),
 * compatível com o TradingView Lightweight Charts que espera timestamps nesse formato.
 *
 * Exemplo de uso no front-end (TradingView):
 * <pre>
 *   { time: 1769731200, open: 37.23, high: 37.98, low: 37.02, close: 37.76 }
 * </pre>
 *
 * Todos os valores monetários usam BigDecimal para evitar perda de precisão.
 */
public record Candle(
        long timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {
}
