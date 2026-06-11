package com.example.rental.service.impl;

import com.example.rental.config.JwtProvider;
import com.example.rental.domain.UserStatus;
import com.example.rental.dto.AuthResponse;
import com.example.rental.dto.LoginRequest;
import com.example.rental.model.OtpToken;
import com.example.rental.model.User;
import com.example.rental.repository.OtpTokenRepository;
import com.example.rental.repository.UserRepository;
import com.example.rental.service.AuthService;
import com.example.rental.service.EmailService;
import com.example.rental.service.utils.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final JwtProvider jwtProvider;

    @Override
    public void sendLoginOtp(String email) throws Exception {
        // Kiem tra email ton tai trong he thong
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("Email chua duoc dang ky hoac khong co quyen truy cap he thong");
        }

        // Kiem tra trang thai tai khoan
        if (user.getTrangThai() == UserStatus.KHOA) {
            throw new Exception("Tai khoan da bi khoa. Vui long lien he quan tri vien");
        }

        // Xoa OTP cu neu ton tai
        OtpToken isExist = otpTokenRepository.findByEmail(email);
        if (isExist != null) {
            otpTokenRepository.delete(isExist);
        }

        // Tao OTP moi
        String otp = OtpUtil.generateOtp();

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setMaXacThuc(otp);
        otpToken.setDaSuDung(false);
        otpToken.setNgayTao(LocalDateTime.now());
        otpToken.setThoiHan(LocalDateTime.now().plusMinutes(5)); // OTP het han sau 5 phut
        otpTokenRepository.save(otpToken);

        // Gui email
        String subject = "Ma OTP dang nhap he thong quan ly nha tro";
        String text = "Ma dang nhap OTP cua ban la: " + otp + "\nMa nay co hieu luc trong 5 phut.";
        emailService.sendOtp(email, otp, subject, text);
    }

    @Override
    public AuthResponse login(LoginRequest req) throws Exception {
        String email = req.getEmail();
        String otp = req.getOtp();

        // Kiem tra user ton tai
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BadCredentialsException("Email khong ton tai trong he thong");
        }

        // Kiem tra trang thai tai khoan
        if (user.getTrangThai() == UserStatus.KHOA) {
            throw new Exception("Tai khoan da bi khoa");
        }

        // Xac thuc OTP
        OtpToken otpToken = otpTokenRepository.findByEmailAndMaXacThuc(email, otp);
        if (otpToken == null) {
            throw new Exception("Ma OTP khong hop le");
        }
        if (otpToken.getDaSuDung()) {
            throw new Exception("Ma OTP da duoc su dung");
        }
        if (otpToken.getThoiHan().isBefore(LocalDateTime.now())) {
            throw new Exception("Ma OTP da het han");
        }

        otpToken.setDaSuDung(true);
        otpTokenRepository.save(otpToken);

        // Tao authentication
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getVaiTro().toString()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Tao JWT token
        String token = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Dang nhap thanh cong");
        authResponse.setRole(user.getVaiTro());

        return authResponse;
    }
}
