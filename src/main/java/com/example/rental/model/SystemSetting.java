package com.example.rental.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "cai_dat_he_thong")
@Data
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String maCaiDat;

    @Column(columnDefinition = "TEXT")
    private String giaTri;

    private String moTa;

    private LocalDateTime ngaySua;
}
