package com.example.rental.dto;

import com.example.rental.domain.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal tienPhong;
    private LocalDate hanThanhToan;
    private InvoiceStatus trangThai;
    private String ghiChu;
    private List<InvoiceServiceItemRequest> dichVu;

    @Data
    public static class InvoiceServiceItemRequest {
        private Long maDichVu;
        private String tenDichVu;
        private BigDecimal soLuong;
        private BigDecimal donGia;
    }
}
