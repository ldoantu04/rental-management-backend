package com.example.rental.controller;

import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.AuthResponse;
import com.example.rental.dto.LoginRequest;
import com.example.rental.dto.SendOtpRequest;
import com.example.rental.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse> sendOtpHandler(@RequestBody SendOtpRequest req) throws Exception {
        authService.sendLoginOtp(req.getEmail());
        ApiResponse res = new ApiResponse();
        res.setMessage("Gui ma OTP thanh cong");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginHandler(@RequestBody LoginRequest req) throws Exception {
        AuthResponse authResponse = authService.login(req);
        return ResponseEntity.ok(authResponse);
    }
}
