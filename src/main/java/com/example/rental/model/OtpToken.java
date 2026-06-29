package com.example.rental.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ma_otp")
@Data
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maNguoiDung")
    private User nguoiDung;

    private String email;

    private String maXacThuc;

    private LocalDateTime thoiHan;

    private Boolean daSuDung = false;

    private LocalDateTime ngayTao;
}
