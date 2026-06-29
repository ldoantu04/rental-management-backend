package com.example.rental.model;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.WaterCalculationType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hoa_don")
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String maHoaDon;

    @ManyToOne
    @JoinColumn(name = "maHopDong")
    private Contract hopDong;

    private LocalDate kyHoaDon;

    private Integer chiSoDienCu;

    private Integer chiSoDienMoi;

    private BigDecimal giaDien;

    private Integer chiSoNuocCu;

    private Integer chiSoNuocMoi;

    private BigDecimal giaNuoc;

    @Enumerated(EnumType.STRING)
    private WaterCalculationType kieuTinhNuoc = WaterCalculationType.CHI_SO;

    private BigDecimal tienPhong;

    private BigDecimal tongTien;

    private BigDecimal phiPhat;

    private LocalDate hanThanhToan;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus trangThai;

    private String ghiChu;

    @ManyToOne
    @JoinColumn(name = "maNguoiTao")
    private User nguoiTao;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;

    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceServiceItem> danhSachDichVu = new ArrayList<>();
}
