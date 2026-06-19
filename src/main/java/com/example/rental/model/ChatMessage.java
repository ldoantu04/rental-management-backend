package com.example.rental.model;

import com.example.rental.domain.ChatMessageRole;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tin_nhan_ai")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "maHoiThoai", nullable = false)
    private ChatConversation hoiThoai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageRole vaiTro;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String noiDung;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String duLieuJson;

    private Boolean canXacNhan = false;

    private String hanhDongChoXacNhan;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payloadHanhDong;

    private Boolean daXuLy = false;

    private String ketQuaHanhDong;

    private LocalDateTime ngayTao;
}
