package com.example.rental.repository;

import com.example.rental.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    OtpToken findByEmail(String email);
    OtpToken findByEmailAndMaXacThuc(String email, String maXacThuc);
    OtpToken findByNguoiDungId(Long nguoiDungId);
}
