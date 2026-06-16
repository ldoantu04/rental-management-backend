package com.example.rental.repository;

import com.example.rental.domain.ContractStatus;
import com.example.rental.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByKhachThueIdAndTrangThai(Long khachThueId, ContractStatus trangThai);
    List<Contract> findByPhongTroIdAndTrangThai(Long phongTroId, ContractStatus trangThai);
    List<Contract> findAllByOrderByNgayTaoDesc();

    @Query("SELECT c FROM Contract c WHERE c.phongTro.id = :phongTroId AND c.trangThai <> :exclude")
    List<Contract> findByPhongTroIdAndTrangThaiNot(@Param("phongTroId") Long phongTroId, @Param("exclude") ContractStatus exclude);
}
