package com.example.rental.dto;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class EmployeeResponse {
    private Long id;
    private String username;
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
    private Set<MotelSimple> assignedMotels;
    private LocalDateTime ngayTao;
    private LocalDateTime ngaySua;

    @Data
    public static class MotelSimple {
        private Long id;
        private String tenTro;
        private String diaChi;
    }

    public static MotelSimple fromMotel(com.example.rental.model.Motel motel) {
        if (motel == null) return null;
        MotelSimple s = new MotelSimple();
        s.setId(motel.getId());
        s.setTenTro(motel.getTenTro());
        s.setDiaChi(motel.getDiaChi());
        return s;
    }
}
