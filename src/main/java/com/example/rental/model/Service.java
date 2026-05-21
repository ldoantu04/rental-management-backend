package com.example.rental.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "danh_muc_dich_vu")
@Data
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenDichVu;

    private BigDecimal giaMacDinh;

    private String donViTinh;

    private Boolean HoatDong;
}
