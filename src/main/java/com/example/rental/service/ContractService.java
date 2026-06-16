package com.example.rental.service;

import com.example.rental.dto.ContractRequest;
import com.example.rental.model.Contract;

import java.util.List;

public interface ContractService {
    Contract createContract(ContractRequest req) throws Exception;
    Contract updateContract(Long id, ContractRequest req) throws Exception;
    void cancelContract(Long id, String lyDoHuy) throws Exception;
    Contract findById(Long id) throws Exception;
    List<Contract> findAll();
}
