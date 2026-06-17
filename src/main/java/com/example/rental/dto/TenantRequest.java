package com.example.rental.dto;

import com.example.rental.domain.Gender;
import com.example.rental.domain.TenantStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TenantRequest {
    private String hoTen;
    private LocalDate ngaySinh;
    private Gender gioiTinh;
    private String cccd;
    private String sdt;
    private String email;
    private String diaChi;
    private String hinhAnh;
    private List<String> anhGiayTo = new ArrayList<>();
    private List<RoommateRequest> danhSachNguoiOCung = new ArrayList<>();
    private Long maPhongTro;
    private LocalDate ngayBatDauThue;
    private BigDecimal tienCoc;
    private TenantStatus trangThai;
    private String ghiChu;
}
