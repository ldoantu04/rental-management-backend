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
<<<<<<< HEAD
    void cancelContract(Long id, String lyDoHuy) throws Exception;
=======
    Contract updateContract(Long id, ContractRequest req, User nguoiSua) throws Exception;
    void cancelContract(Long id, String lyDoHuy, User currentUser) throws Exception;
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
    Contract findById(Long id) throws Exception;
    Contract findById(Long id, User currentUser) throws Exception;
    List<Contract> findAll();
    List<Contract> findAll(User currentUser);
    List<Contract> findByTrangThai(ContractStatus trangThai);
    List<Contract> findByTrangThai(ContractStatus trangThai, User currentUser);
    List<Contract> findByPhongTroIdAndTrangThai(Long phongTroId, ContractStatus trangThai);
}
