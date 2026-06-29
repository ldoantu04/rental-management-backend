package com.example.rental.model;

import com.example.rental.domain.MotelStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nha_tro")
@Data
public class Motel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenTro;

    private String diaChi;

    private Integer soTang;

    private Integer tongPhong;

    @Enumerated(EnumType.STRING)
    private MotelStatus trangThai;

    private String ghiChu;

    @ManyToOne
    @JoinColumn(name = "maNguoiTao")
    private User nguoiTao;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;

    @OneToMany(mappedBy = "nhaTro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Service> danhSachDichVu = new ArrayList<>();
}
