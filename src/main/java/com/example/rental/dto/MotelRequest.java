package com.example.rental.dto;

import com.example.rental.domain.MotelStatus;
import lombok.Data;

@Data
public class MotelRequest {
    private String tenTro;
    private String diaChi;
    private Integer soTang;
    private Integer tongPhong;
    private MotelStatus trangThai;
    private String ghiChu;
}
