package com.example.rental.repository;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTrangThai(InvoiceStatus trangThai);
    List<Invoice> findByHopDongKhachThueId(Long khachThueId);
}
