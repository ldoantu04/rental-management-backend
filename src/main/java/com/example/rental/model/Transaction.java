package com.example.rental.model;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "giao_dich")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String maGiaoDich;

    @ManyToOne
    @JoinColumn(name = "maHoaDon")
    private Invoice hoaDon;

    private BigDecimal soTien;

    @Enumerated(EnumType.STRING)
    private PaymentMethod hinhThucTT;

    private String maCongTT;

    private String duLieuTT;

    @Enumerated(EnumType.STRING)
    private PaymentStatus trangThai;

    private LocalDateTime ngayThanhToan;

    private String ghiChu;

    private LocalDateTime ngayTao;
}
