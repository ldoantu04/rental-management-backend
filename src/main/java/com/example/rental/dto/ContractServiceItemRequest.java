package com.example.rental.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractServiceItemRequest {
    private String tenDichVu;
    private String kieuTinh;
    private BigDecimal donGia;
    private Boolean laDichVuBoSung = false;
}
