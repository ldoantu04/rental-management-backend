package com.example.rental.model;

import com.example.rental.domain.RoomStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phong_tro")
@Data
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maNhaTro")
    private Motel nhaTro;

    private String maPhong;

    private BigDecimal dienTich;

    private BigDecimal giaThue;

    private Integer soNguoi;

    private Integer tang;

    @Enumerated(EnumType.STRING)
    private RoomStatus trangThai;

    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;
}
