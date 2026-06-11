package com.example.rental.repository;

import com.example.rental.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findBySdt(String sdt);
    List<User> findByHoTenContainingIgnoreCaseOrEmailContainingIgnoreCaseOrSdtContaining(
            String hoTen, String email, String sdt);
    List<User> findAllByOrderByNgayTaoDesc();
}

