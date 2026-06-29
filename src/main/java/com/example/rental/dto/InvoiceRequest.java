package com.example.rental.dto;

import com.example.rental.domain.InvoiceStatus;
<<<<<<< HEAD
=======
import com.example.rental.domain.WaterCalculationType;
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
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
<<<<<<< HEAD
    private BigDecimal tienPhong;
=======
    private WaterCalculationType kieuTinhNuoc;
    private BigDecimal tienPhong;
    private BigDecimal tongTien;
    private BigDecimal phiPhat;
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
    private LocalDate hanThanhToan;
    private InvoiceStatus trangThai;
    private String ghiChu;
    private List<InvoiceServiceItemRequest> danhSachDichVu = new ArrayList<>();
}
