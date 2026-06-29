package com.example.rental.model;

import com.example.rental.domain.ChatConversationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "hoi_thoai_ai")
@Data
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tieuDe;

    @ManyToOne
    @JoinColumn(name = "maNguoiDung", nullable = false)
    private User nguoiDung;

    @Enumerated(EnumType.STRING)
    private ChatConversationStatus trangThai = ChatConversationStatus.DANG_HOAT_DONG;

    private LocalDateTime ngayTao;

    private LocalDateTime ngaySua;
}
