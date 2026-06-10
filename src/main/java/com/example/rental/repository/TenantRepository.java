package com.example.rental.repository;

import com.example.rental.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Tenant findByCccd(String cccd);

    @Query("""
    SELECT t FROM Tenant t
    WHERE LOWER(t.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR t.cccd LIKE CONCAT('%', :keyword, '%')
    """)
    List<Tenant> search(@Param("keyword") String keyword);

    List<Tenant> findAllByOrderByNgayTaoDesc();
}
