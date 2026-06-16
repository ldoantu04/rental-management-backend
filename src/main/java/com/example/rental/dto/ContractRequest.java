package com.example.rental.dto;

import com.example.rental.domain.ContractStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private Integer ngayThanhToan;
    private String dieuKhoan;
    private String dichVu;
    private String fileHopDong;
    private ContractStatus trangThai;
    private String lyDoHuy;
    private List<ContractServiceItemRequest> danhSachDichVu = new ArrayList<>();
}
