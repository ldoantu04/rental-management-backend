package com.example.rental.repository;

import com.example.rental.domain.TenantStatus;
import com.example.rental.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Tenant findByCccd(String cccd);

    List<Tenant> findAllByOrderByNgayTaoDesc();

    List<Tenant> findByTrangThaiOrderByNgayTaoDesc(TenantStatus trangThai);
}
