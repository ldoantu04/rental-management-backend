package com.example.rental.dto;

import com.example.rental.domain.Gender;
import lombok.Data;

import java.time.LocalDate;

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
    private String ghiChu;
}
