package com.example.rental.service;

import com.example.rental.domain.TenantStatus;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;

import java.util.List;

public interface TenantService {
    Tenant createTenant(TenantRequest req) throws Exception;
    Tenant updateTenant(Long id, TenantRequest req) throws Exception;
    void moveOutTenant(Long id) throws Exception;
    Tenant findById(Long id) throws Exception;
    List<Tenant> findAll();
    List<Tenant> findByTrangThai(TenantStatus trangThai);
<<<<<<< HEAD
=======

    Tenant createTenant(TenantRequest req, User nguoiTao) throws Exception;
    Tenant updateTenant(Long id, TenantRequest req, User nguoiSua) throws Exception;
    void moveOutTenant(Long id, User nguoiThucHien) throws Exception;
    Tenant findById(Long id, User currentUser) throws Exception;
    List<Tenant> findAll(User currentUser);
    List<Tenant> findByTrangThai(TenantStatus trangThai, User currentUser);
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
}
