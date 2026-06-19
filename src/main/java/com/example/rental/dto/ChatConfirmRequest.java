package com.example.rental.dto;

import lombok.Data;

@Data
public class ChatConfirmRequest {
    private Long hoiThoaiId;
    private Long tinNhanId;
    private Boolean xacNhan;
}
