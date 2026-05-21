package com.example.rental.model;

import com.example.rental.domain.Gender;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_thue")
@Data
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hoTen;

    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    private Gender gioiTinh;

    @Column(unique = true)
    private String cccd;

    private String sdt;

    private String email;

    private String diaChi;

    private String hinhAnh;

    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;
}
