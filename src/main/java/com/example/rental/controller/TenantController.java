package com.example.rental.controller;

import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Tenant;
import com.example.rental.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody TenantRequest req) throws Exception {
        Tenant tenant = tenantService.createTenant(req);
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable Long id,
            @RequestBody TenantRequest req) throws Exception {
        Tenant tenant = tenantService.updateTenant(id, req);
        return ResponseEntity.ok(tenant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTenant(@PathVariable Long id) throws Exception {
        tenantService.deleteTenant(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa khach thue thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable Long id) throws Exception {
        Tenant tenant = tenantService.findById(id);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = tenantService.findAll();
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Tenant>> searchTenants(@RequestParam String keyword) {
        List<Tenant> tenants = tenantService.search(keyword);
        return ResponseEntity.ok(tenants);
    }
}
