package com.example.rental.repository;

import com.example.rental.model.Motel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MotelRepository extends JpaRepository<Motel, Long> {
    Motel findByTenTro(String tenTro);
    List<Motel> findByNguoiTaoId(Long nguoiTaoId);
    List<Motel> findByNguoiTaoIdOrderByIdDesc(Long nguoiTaoId);

    @Query("SELECT m FROM Motel m LEFT JOIN FETCH m.nguoiTao ORDER BY m.id DESC")
    List<Motel> findAllOrderByIdDesc();

    @Query("""
    SELECT m FROM Motel m
    WHERE LOWER(m.tenTro) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(m.diaChi) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Motel> search(@Param("keyword") String keyword);
}
