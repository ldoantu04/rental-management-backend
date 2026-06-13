package com.example.rental.dto;

import com.example.rental.domain.ContractStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ContractRequest {
    private Long maKhachThue;
    private Long maPhongTro;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private BigDecimal tienCoc;
    private BigDecimal giaThue;
    private Integer chuKyThanhToan;
    private String dieuKhoan;
    private ContractStatus trangThai;
    private String lyDoHuy;
}
