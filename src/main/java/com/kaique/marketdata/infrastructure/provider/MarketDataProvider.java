package com.kaique.marketdata.infrastructure.provider;

import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.model.MarketData;

/**
 * Contrato que todo provedor de dados de mercado deve implementar.
 * O Strategy Pattern é aplicado aqui: o MarketDataService não conhece nenhuma implementação,
 * ele só conhece esta interface.
 */
public interface MarketDataProvider {

    /**
     * Busca o preço atual de um ativo pelo seu símbolo.
     *
     * @param symbol o ticker ou identificador do ativo (ex: "bitcoin", "PETR4.SA")
     * @return MarketData com as informações de preço e mercado
     */
    MarketData fetchCurrentPrice(String symbol);

    /**
     * Indica se este provider suporta o tipo de mercado informado.
     *
     * @param marketType o tipo de mercado (CRYPTO, STOCK, FII)
     * @return true se o provider é capaz de atender requisições deste tipo
     */
    boolean supports(MarketType marketType);
}
