package com.example.rental.dto;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {
    private String hoTen;
    private String email;
    private String sdt;
    private LocalDate ngaySinh;
    private String diaChi;
    private UserRole vaiTro;
    private String phamViQuanLy;
    private UserStatus trangThai;
    private String ghiChu;
}
