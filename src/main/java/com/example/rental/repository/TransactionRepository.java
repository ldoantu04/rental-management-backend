package com.example.rental.repository;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByMaGiaoDich(String maGiaoDich);
    List<Transaction> findByHoaDonId(Long hoaDonId);
    List<Transaction> findByTrangThai(PaymentStatus trangThai);
    List<Transaction> findByHinhThucTT(PaymentMethod hinhThucTT);
    List<Transaction> findByNgayTaoBetween(LocalDateTime from, LocalDateTime to);
    List<Transaction> findAllByOrderByNgayTaoDesc();
}
