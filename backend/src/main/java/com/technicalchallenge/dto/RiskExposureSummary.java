package com.technicalchallenge.dto;

import java.math.BigDecimal;

public class RiskExposureSummary {
    private BigDecimal totalRisk;
    private BigDecimal netRisk;
    private BigDecimal grossRisk;
    private BigDecimal dv01; //interest rate risk measure, dollar value of a one basis point move
    private BigDecimal pv; //present value of the trades
    private BigDecimal npv; //net present value of the trades

    // Getters and Setters
}
