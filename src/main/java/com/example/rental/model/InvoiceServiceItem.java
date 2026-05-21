package com.example.rental.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_dich_vu")
@Data
public class InvoiceServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maHoaDon")
    private Invoice hoaDon;

    @ManyToOne
    @JoinColumn(name = "maDichVu")
    private Service danhMucDichVu;

    private String tenDichVu;

    private BigDecimal soLuong;

    private BigDecimal donGia;

    private BigDecimal thanhTien;
}
