package com.example.rental.model;

import com.example.rental.domain.NotificationType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maNguoiDung")
    private User nguoiDung;

    private String tieuDe;

    @Column(columnDefinition = "TEXT")
    private String noiDung;

    @Enumerated(EnumType.STRING)
    private NotificationType loai;

    private Long maThamChieu;

    private Boolean daDoc;

    private LocalDateTime ngayTao;
}
