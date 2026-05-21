package com.example.rental.model;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name= "nguoi_dung")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hoTen;

    @Column(unique = true, nullable = false)
    private String email;

    private String sdt;

    private LocalDate ngaySinh;

    private String diaChi;

    @Enumerated(EnumType.STRING)
    private UserRole vaiTro;

    private String phamViQuanLy;

    @Enumerated(EnumType.STRING)
    private UserStatus trangThai;

    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;

}
