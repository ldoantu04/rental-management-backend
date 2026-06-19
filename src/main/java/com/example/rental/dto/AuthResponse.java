package com.example.rental.dto;

import com.example.rental.domain.UserRole;
import lombok.Data;

@Data
public class AuthResponse {
    private String jwt;
    private String message;
    private UserRole role;
    private Long userId;
    private String hoTen;
}
