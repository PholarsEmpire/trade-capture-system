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
public class DailySummaryDTO {
    private String date;
    private long todaysTradeCount;
    private BigDecimal todaysTotalNotional;
    private BigDecimal todaysAverageNotional;
    private long yesterdayTradeCount;
    private BigDecimal yesterdayTotalNotional;
    private BigDecimal yesterdayAverageNotional;
    private long todaysNewTrades;
    private long todaysAmendedTrades;
    private long todaysTerminatedTrades;
    private BigDecimal dayOverDayChangePercentage; // percentage change in trade count

    // USER-SPECIFIC PERFORMANCE METRICS
    private Map<String, Long> todaysTradesByLoggedInTrader;
    private Map<String, BigDecimal> todaysNotionalByLoggedInTrader;
    private Map<String, Long> tradesByTrader;
    private Map<String, BigDecimal> notionalByTrader;
    private Map<String, Long> tradesByInputter;
    
    // BOOK-LEVEL ACTIVITY SUMMARIES
    private Map<String, Long> tradesByBook;
    private Map<String, BigDecimal> notionalByBook;
    private Map<String, Map<String, Long>> statusByBook; // Book -> Status -> Count
    }
