package com.technicalchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeSummaryDTO {
    private long totalTrades;
    private Map<String, Long> tradesByStatus;        // e.g. {"LIVE": 12, "AMENDED": 3}
    private Map<String, BigDecimal> notionalByCurrency; // e.g. {"USD": 12_000_000, "GBP": 5_000_000}
    private Map<String, Long> tradesByCounterparty;  // e.g. {"Barclays": 5, "JP Morgan": 2}
    private Map<String, Long> tradesByType;          // e.g. {"Swap": 7, "Forward": 3}
}
