package com.example.rental.dto;

import com.example.rental.domain.ChatConversationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationDto {
    private Long id;
    private String tieuDe;
    private Long nguoiDungId;
    private ChatConversationStatus trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngaySua;
    private String tinNhanMoiNhat;
}
