package com.technicalchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeSummaryDTO {
    private long totalTrades;
    private Map<String, Long> tradesByStatus;        // e.g. {"LIVE": 12, "AMENDED": 3}
    private Map<String, Long> tradesByCurrency;      // e.g. {"USD": 8, "EUR": 4}
    private Map<String, BigDecimal> notionalByCurrency; // e.g. {"USD": 12_000_000, "GBP": 5_000_000}
    private Map<String, BigDecimal> notionalByType; // e.g. {"Swap": 15_000_000, "Forward": 2_000_000}
    private Map<String, BigDecimal> notionalByCounterparty;  // e.g. {"BigBank": 7_000_000, "MegaFund": 10_000_000}
    private Map<String, Long> tradesByCounterparty;  // e.g. {"Barclays": 5, "JP Morgan": 2}
    private Map<String, Long> tradesByType;          // e.g. {"Swap": 7, "Forward": 3}
    private Map<String, Long> tradesBySubType;       // e.g. {"Interest Rate Swap": 4, "Currency Swap": 3}

    //Risk exposure summary details
    private Map<String, BigDecimal> netExposureByCounterparty;
    private Map<String, BigDecimal> grossExposureByCounterparty;
    private Map<String, BigDecimal> netExposureByCurrency;
    private Map<String, BigDecimal> grossExposureByCurrency;
    private Map<String, BigDecimal> netExposureByBook;
    private Map<String, BigDecimal> grossExposureByBook;
    private BigDecimal totalNetExposure;
    private BigDecimal totalGrossExposure;
}
