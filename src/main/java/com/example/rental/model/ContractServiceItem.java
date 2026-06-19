package com.example.rental.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "hop_dong_dich_vu")
@Data
public class ContractServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maHopDong")
    @JsonIgnore
    private Contract hopDong;

    private String tenDichVu;

    private String kieuTinh;

    private BigDecimal donGia;

    /** Quantity consumed (1 for flat-rate services). Defaults to 1 when null. */
    private BigDecimal soLuong;

    private Boolean laDichVuBoSung = false;
}
