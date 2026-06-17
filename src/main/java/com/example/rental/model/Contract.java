package com.example.rental.model;

import com.example.rental.domain.ContractStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private Integer ngayThanhToan;

    private String dieuKhoan;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dichVu;

    private String fileHopDong;

    @Enumerated(EnumType.STRING)
    private ContractStatus trangThai;

    private String lyDoHuy;

    private LocalDate ngayHuy;

    @ManyToOne
    @JoinColumn(name = "maNguoiTao")
    @JsonIgnore
    private User nguoiTao;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;

    @OneToMany(mappedBy = "hopDong", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ContractServiceItem> danhSachDichVu = new ArrayList<>();

    @Transient
    public String getTrangThaiHienThi() {

        if (trangThai == ContractStatus.DA_HUY || trangThai == ContractStatus.DA_HET_HAN) {
            return trangThai.name();
        }
        if (ngayKetThuc == null) {
            return ContractStatus.DANG_HIEU_LUC.name();
        }
        LocalDate today = LocalDate.now();
        if (ngayKetThuc.isBefore(today)) {
            return ContractStatus.DA_HET_HAN.name();
        }
        if (ngayKetThuc.minusDays(30).isBefore(today)) {
            return "SAP_HET_HAN";
        }
        return ContractStatus.DANG_HIEU_LUC.name();
    }
}
