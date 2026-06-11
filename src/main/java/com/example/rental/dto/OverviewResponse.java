package com.example.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OverviewResponse {
    private BigDecimal tongDoanhThu;
    private int phongDangThue;
    private int tongPhong;
    private int hoaDonChuaThanhToan;
    private int hopDongSapHetHan;
    private List<HopDongSapHet> hopDongSapHet;
    private List<KhachTreHan> khachTreHanThanhToan;

    @Data
    public static class HopDongSapHet {
        private String tenKhach;
        private String tenTro;
        private String maPhong;
        private String ngayHetHan;
    }

    @Data
    public static class KhachTreHan {
        private String tenKhach;
        private String tenTro;
        private String maPhong;
        private BigDecimal soTien;
        private long soNgayTre;
    }
}
