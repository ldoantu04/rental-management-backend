package com.example.rental.service;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    Transaction findById(Long id) throws Exception;
    Transaction findByMaGiaoDich(String maGiaoDich) throws Exception;
    List<Transaction> findAll();
    List<Transaction> findByInvoiceId(Long hoaDonId);
    List<Transaction> search(String maGiaoDich, Long hoaDonId, PaymentStatus trangThai,
                             PaymentMethod hinhThucTT, LocalDateTime tuNgay, LocalDateTime denNgay);
    void deleteTransaction(Long id) throws Exception;
}
