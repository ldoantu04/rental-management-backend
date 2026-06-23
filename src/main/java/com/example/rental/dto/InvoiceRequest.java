package com.example.rental.dto;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.WaterCalculationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class InvoiceRequest {
    private Long maHopDong;
    private LocalDate kyHoaDon;
    private Integer chiSoDienCu;
    private Integer chiSoDienMoi;
    private BigDecimal giaDien;
    private Integer chiSoNuocCu;
    private Integer chiSoNuocMoi;
    private BigDecimal giaNuoc;
    private WaterCalculationType kieuTinhNuoc;
    private BigDecimal tienPhong;
    private BigDecimal tongTien;
    private BigDecimal phiPhat;
    private LocalDate hanThanhToan;
    private InvoiceStatus trangThai;
    private String ghiChu;
    private List<InvoiceServiceItemRequest> danhSachDichVu = new ArrayList<>();
}
