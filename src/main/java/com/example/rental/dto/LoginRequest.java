package com.example.rental.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String otp;
}
