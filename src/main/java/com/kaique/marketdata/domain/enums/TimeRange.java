package com.kaique.marketdata.domain.enums;

/**
 * Enum que define os ranges de tempo disponíveis para consulta de histórico.
 *
 * Cada valor mapeia para os parâmetros específicos de cada API:
 *
 * <table>
 *   <tr><th>TimeRange</th><th>Brapi (range/interval)</th><th>CoinGecko (days)</th></tr>
 *   <tr><td>1D</td><td>1d / 5m</td><td>1</td></tr>
 *   <tr><td>1W</td><td>5d / 15m</td><td>7</td></tr>
 *   <tr><td>1M</td><td>1mo / 1d</td><td>30</td></tr>
 *   <tr><td>3M</td><td>3mo / 1d</td><td>90</td></tr>
 *   <tr><td>6M</td><td>6mo / 1d</td><td>180</td></tr>
 *   <tr><td>1Y</td><td>1y / 1wk</td><td>365</td></tr>
 *   <tr><td>5Y</td><td>5y / 1mo</td><td>max</td></tr>
 * </table>
 */
public enum TimeRange {

    ONE_DAY("1d", "5m", "1"),
    ONE_WEEK("5d", "15m", "7"),
    ONE_MONTH("1mo", "1d", "30"),
    THREE_MONTHS("3mo", "1d", "90"),
    SIX_MONTHS("6mo", "1d", "180"),
    ONE_YEAR("1y", "1wk", "365"),
    FIVE_YEARS("5y", "1mo", "max");

    private final String brapiRange;
    private final String brapiInterval;
    private final String coinGeckoDays;

    TimeRange(String brapiRange, String brapiInterval, String coinGeckoDays) {
        this.brapiRange = brapiRange;
        this.brapiInterval = brapiInterval;
        this.coinGeckoDays = coinGeckoDays;
    }

    public String getBrapiRange() {
        return brapiRange;
    }

    public String getBrapiInterval() {
        return brapiInterval;
    }

    public String getCoinGeckoDays() {
        return coinGeckoDays;
    }

    /**
     * Converte strings amigáveis (1d, 1w, 1m, 3m, 6m, 1y, 5y) para o enum.
     * Case-insensitive.
     *
     * @param value string do range (ex: "1m", "1y", "5y")
     * @return TimeRange correspondente
     * @throws IllegalArgumentException se o valor não for reconhecido
     */
    public static TimeRange fromString(String value) {
        return switch (value.toUpperCase().trim()) {
            case "1D" -> ONE_DAY;
            case "1W" -> ONE_WEEK;
            case "1M" -> ONE_MONTH;
            case "3M" -> THREE_MONTHS;
            case "6M" -> SIX_MONTHS;
            case "1Y" -> ONE_YEAR;
            case "5Y" -> FIVE_YEARS;
            default -> throw new IllegalArgumentException(
                    "TimeRange inválido: '" + value + "'. Valores aceitos: 1d, 1w, 1m, 3m, 6m, 1y, 5y");
        };
    }
}
