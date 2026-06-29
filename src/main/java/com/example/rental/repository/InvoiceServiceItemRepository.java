package com.example.rental.repository;

import com.example.rental.model.InvoiceServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceServiceItemRepository extends JpaRepository<InvoiceServiceItem, Long> {
    List<InvoiceServiceItem> findByHoaDonId(Long hoaDonId);

    @Modifying
    @Query("DELETE FROM InvoiceServiceItem i WHERE i.hoaDon.id = :hoaDonId")
    void deleteByHoaDonId(@Param("hoaDonId") Long hoaDonId);
}
