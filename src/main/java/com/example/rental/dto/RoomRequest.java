package com.example.rental.dto;

import com.example.rental.domain.RoomStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomRequest {
    private Long maNhaTro;
    private String maPhong;
    private BigDecimal dienTich;
    private BigDecimal giaThue;
    private Integer soNguoi;
    private Integer tang;
    private RoomStatus trangThai;
    private String ghiChu;
}
