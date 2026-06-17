package com.example.rental.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceServiceItemRequest {
    private String tenDichVu;
    private String kieuTinh;
    private BigDecimal soLuong;
    private BigDecimal donGia;
    private Boolean laTuHopDong;
}
