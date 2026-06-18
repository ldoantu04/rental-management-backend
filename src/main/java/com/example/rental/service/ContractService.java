package com.example.rental.service;

import com.example.rental.domain.ContractStatus;
import com.example.rental.dto.ContractRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.User;

import java.util.List;

public interface ContractService {
    Contract createContract(ContractRequest req) throws Exception;
    Contract createContract(ContractRequest req, User nguoiTao) throws Exception;
    Contract updateContract(Long id, ContractRequest req) throws Exception;
    Contract updateContract(Long id, ContractRequest req, User nguoiSua) throws Exception;
    void cancelContract(Long id, String lyDoHuy) throws Exception;
    Contract findById(Long id) throws Exception;
    List<Contract> findAll();
    List<Contract> findByTrangThai(ContractStatus trangThai);
    List<Contract> findByPhongTroIdAndTrangThai(Long phongTroId, ContractStatus trangThai);
}
