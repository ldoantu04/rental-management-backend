package com.example.rental.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceServiceItemRequest {
    private String tenDichVu;
<<<<<<< HEAD
    private BigDecimal soLuong;
    private BigDecimal donGia;
=======
    private String kieuTinh;
    private BigDecimal soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private Boolean laTuHopDong;
    private Integer chiSoDau;
    private Integer chiSoCuoi;
    private String loaiDichVu; // DIEN, NUOC, PHONG, DICH_VU
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
}
