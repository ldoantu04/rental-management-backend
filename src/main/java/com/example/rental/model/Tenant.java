package com.example.rental.model;

import com.example.rental.domain.Gender;
import com.example.rental.domain.TenantStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(name = "khach_thue_anh_giay_to", joinColumns = @JoinColumn(name = "maKhachThue"))
    @Column(name = "urlAnh")
    private List<String> anhGiayTo = new ArrayList<>();

    @OneToMany(mappedBy = "khachThue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Roommate> danhSachNguoiOCung = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "maPhongTro")
    @JsonIgnore
    private Room phongTro;

    private LocalDate ngayBatDauThue;

    private BigDecimal tienCoc;

    @Enumerated(EnumType.STRING)
    private TenantStatus trangThai;

    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;
}
