package com.example.rental.repository;

import com.example.rental.model.ContractServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractServiceItemRepository extends JpaRepository<ContractServiceItem, Long> {

    @Modifying
    @Query("DELETE FROM ContractServiceItem c WHERE c.hopDong.id = :hopDongId")
    void deleteByHopDongId(@Param("hopDongId") Long hopDongId);
}
