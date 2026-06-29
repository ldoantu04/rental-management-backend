package com.example.rental.repository;

import com.example.rental.model.InvoiceServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
=======
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b

import java.util.List;

public interface InvoiceServiceItemRepository extends JpaRepository<InvoiceServiceItem, Long> {
    List<InvoiceServiceItem> findByHoaDonId(Long hoaDonId);
<<<<<<< HEAD
    void deleteByHoaDonId(Long hoaDonId);
=======

    @Modifying
    @Query("DELETE FROM InvoiceServiceItem i WHERE i.hoaDon.id = :hoaDonId")
    void deleteByHoaDonId(@Param("hoaDonId") Long hoaDonId);
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
}
