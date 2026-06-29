package com.example.rental.model;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    private String cccd;

    private LocalDate ngayVaoLam;

    @Enumerated(EnumType.STRING)
    private UserRole vaiTro;

    @ManyToMany
    @JoinTable(
            name = "nhan_vien_nha_tro",
            joinColumns = @JoinColumn(name = "maNhanVien"),
            inverseJoinColumns = @JoinColumn(name = "maNhaTro")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Motel> assignedMotels = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private UserStatus trangThai;

    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;
}
