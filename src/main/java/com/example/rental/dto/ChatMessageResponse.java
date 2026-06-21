package com.example.rental.dto;

import com.example.rental.domain.ChatMessageRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long hoiThoaiId;
    private ChatMessageRole vaiTro;
    private String noiDung;
    private String duLieuJson;
    private Boolean canXacNhan;
    private String hanhDongChoXacNhan;
    private String payloadHanhDong;
    private Boolean daXuLy;
    private String ketQuaHanhDong;
    private LocalDateTime ngayTao;
}
