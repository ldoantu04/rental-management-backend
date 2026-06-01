package com.example.rental.service;

import com.example.rental.dto.AuthResponse;
import com.example.rental.dto.LoginRequest;

public interface AuthService {
    void sendLoginOtp(String email) throws Exception;
    AuthResponse login(LoginRequest req) throws Exception;
}
