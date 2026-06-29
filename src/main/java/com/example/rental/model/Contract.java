package com.example.rental.model;

import com.example.rental.domain.ContractStatus;
<<<<<<< HEAD
=======
import com.example.rental.domain.WaterCalculationType;
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
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

    /** Default electricity price (VND / kWh) agreed in this contract. */
    @Column(precision = 18, scale = 2)
    private BigDecimal giaDien;

    /** Default water price (VND / m3 or VND / month depending on kieuTinhNuoc). */
    @Column(precision = 18, scale = 2)
    private BigDecimal giaNuoc;

    @Enumerated(EnumType.STRING)
    private WaterCalculationType kieuTinhNuoc = WaterCalculationType.CHI_SO;

    private Integer chuKyThanhToan;

    private Integer ngayThanhToan;

    private String dieuKhoan;

<<<<<<< HEAD
    @Lob
    @Column(columnDefinition = "TEXT")
    private String dichVu;

=======
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
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

<<<<<<< HEAD
    @OneToMany(mappedBy = "hopDong", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
=======
    @OneToMany(mappedBy = "hopDong", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
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
