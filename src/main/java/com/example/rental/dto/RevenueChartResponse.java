package com.example.rental.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevenueChartResponse {
    private int month;
    private BigDecimal revenue;
}
