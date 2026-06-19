package com.example.rental.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevenueChartDTO {
    private int month;
    private BigDecimal revenue;
}
