package com.example.rental.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatSendResponse {
    private Long hoiThoaiId;
    private ChatMessageResponse tinNhanNguoiDung;
    private ChatMessageResponse tinNhanTroLy;
    private List<ChatMessageResponse> toanBoTinNhan;
}
