package com.example.rental.repository;

import com.example.rental.domain.ContractStatus;
import com.example.rental.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByKhachThueId(Long khachThueId);
    List<Contract> findByKhachThueIdAndTrangThai(Long khachThueId, ContractStatus trangThai);
}
