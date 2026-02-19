package com.kaique.marketdata.presentation.controller;

import com.kaique.marketdata.application.service.MarketDataService;
import com.kaique.marketdata.domain.enums.MarketType;
import com.kaique.marketdata.domain.enums.TimeRange;
import com.kaique.marketdata.domain.model.Candle;
import com.kaique.marketdata.domain.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para consultas de dados de mercado.
 *
/**
 * Controller REST para consultas de dados de mercado.
 *
 * Exemplos:
 *   GET /market/CRYPTO/bitcoin
 *   GET /market/STOCK/PETR4.SA
 *   GET /market/STOCK/IBM
 *   GET /market/STOCK/PETR4.SA/history?range=1m
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

    @GetMapping("/{type}/{symbol}/history")
    public ResponseEntity<List<Candle>> getHistory(
            @PathVariable("type") String type,
            @PathVariable("symbol") String symbol,
            @RequestParam(value = "range", defaultValue = "1m") String range) {

        log.info("GET /market/{}/{}/history?range={}", type, symbol, range);

        MarketType marketType = MarketType.valueOf(type.toUpperCase());
        TimeRange timeRange = TimeRange.fromString(range);
        List<Candle> history = marketDataService.getHistory(marketType, symbol, timeRange);

        return ResponseEntity.ok(history);
    }
}
