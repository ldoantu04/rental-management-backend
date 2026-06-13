package com.example.rental.repository;

import com.example.rental.model.InvoiceServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceServiceItemRepository extends JpaRepository<InvoiceServiceItem, Long> {
    List<InvoiceServiceItem> findByHoaDonId(Long hoaDonId);
    void deleteByHoaDonId(Long hoaDonId);
}
