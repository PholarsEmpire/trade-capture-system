package com.technicalchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryDTO {
    private String date;
    private long tradeCount;
    private BigDecimal totalNotional;
    private BigDecimal averageNotional;
    private long newTrades;
    private long amendedTrades;
    private long terminatedTrades;
    private BigDecimal dayOverDayChange; // percentage change in trade count
}
