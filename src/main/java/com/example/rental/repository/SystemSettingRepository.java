package com.example.rental.repository;

import com.example.rental.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findByMaCaiDat(String maCaiDat);
    boolean existsByMaCaiDat(String maCaiDat);
}
