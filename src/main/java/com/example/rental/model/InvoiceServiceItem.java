package com.example.rental.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Invoice hoaDon;

    @ManyToOne
    @JoinColumn(name = "maDichVu")
    @JsonIgnore
    private Service dichVu;

    private String tenDichVu;

    private String kieuTinh;

    private BigDecimal soLuong;

    private BigDecimal donGia;

    private BigDecimal thanhTien;

    private Boolean laTuHopDong = true;
    private Integer chiSoDau;
    private Integer chiSoCuoi;
    private String loaiDichVu; // DIEN, NUOC, PHONG, DICH_VU
}
