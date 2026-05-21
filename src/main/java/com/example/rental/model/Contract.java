package com.example.rental.model;

import com.example.rental.domain.ContractStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hop_dong")
@Data
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String maHopDong;

    @ManyToOne
    @JoinColumn(name = "maKhachThue")
    private Tenant khachThue;

    @ManyToOne
    @JoinColumn(name = "maPhongTro")
    private Room phongTro;

    private LocalDate ngayBatDau;

    private LocalDate ngayKetThuc;

    private BigDecimal tienCoc;

    private BigDecimal giaThue;

    private Integer chuKyThanhToan;

    private String dieuKhoan;

    @Enumerated(EnumType.STRING)
    private ContractStatus trangThai;

    private String lyDoHuy;

    private LocalDate ngayHuy;

    @ManyToOne
    @JoinColumn(name = "maNguoiTao")
    private User nguoiTao;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;
}
