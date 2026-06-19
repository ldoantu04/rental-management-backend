package com.example.rental.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatSendResponse {
    private Long hoiThoaiId;
    private ChatMessageDto tinNhanNguoiDung;
    private ChatMessageDto tinNhanTroLy;
    private List<ChatMessageDto> toanBoTinNhan;
}
