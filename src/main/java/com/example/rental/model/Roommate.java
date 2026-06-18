package com.example.rental.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_o_cung")
@Data
public class Roommate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maKhachThue")
    @JsonIgnore
    private Tenant khachThue;

    private String hoTen;

    private String quanHe;

    private String cccd;

    private String sdt;

    private LocalDateTime ngayTao;
}
