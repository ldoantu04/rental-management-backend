package com.example.rental.repository;

import com.example.rental.model.Motel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotelRepository extends JpaRepository<Motel, Long> {
    Motel findByTenTro(String tenTro);
}
