package com.example.rental.service;

import com.example.rental.domain.TenantStatus;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Tenant;

import java.util.List;

public interface TenantService {
    Tenant createTenant(TenantRequest req) throws Exception;
    Tenant updateTenant(Long id, TenantRequest req) throws Exception;
    void moveOutTenant(Long id) throws Exception;
    Tenant findById(Long id) throws Exception;
    List<Tenant> findAll();
    List<Tenant> findByTrangThai(TenantStatus trangThai);
}
