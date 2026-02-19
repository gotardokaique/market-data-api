package com.kaique.marketdata.presentation.controller;

import com.kaique.marketdata.application.service.MarketDataService;
import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para consultas de dados de mercado.
 *
 * Endpoints:
 *   GET /market/{type}/{symbol}
 *
 * Exemplos:
 *   GET /market/CRYPTO/bitcoin       → Preço do Bitcoin via CoinGecko
 *   GET /market/STOCK/PETR4.SA       → Preço da Petrobras via Yahoo Finance
 *   GET /market/FII/HGLG11.SA        → Preço do HGLG11 via Yahoo Finance
 */
@RestController
@RequestMapping("/market")
public class MarketDataController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/{type}/{symbol}")
    public ResponseEntity<MarketData> getCurrentPrice(
            @PathVariable("type") String type,
            @PathVariable("symbol") String symbol) {

        log.info("GET /market/{}/{}", type, symbol);

        MarketType marketType = MarketType.valueOf(type.toUpperCase());
        MarketData data = marketDataService.getCurrentPrice(marketType, symbol);

        return ResponseEntity.ok(data);
    }
}
