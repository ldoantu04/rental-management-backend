package com.example.rental.dto;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeRequest {
    private String username;
    private String password;
    private String hoTen;
    private String email;
    private String sdt;
    private LocalDate ngaySinh;
    private String diaChi;
    private String cccd;
    private LocalDate ngayVaoLam;
    private UserRole vaiTro;
    private UserStatus trangThai;
    private String ghiChu;
    private List<Long> assignedMotelIds;
}
