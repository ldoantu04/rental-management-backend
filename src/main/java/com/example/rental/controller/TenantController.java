package com.example.rental.controller;

import com.example.rental.domain.TenantStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;
import com.example.rental.service.TenantService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Tenant> createTenant(
            @RequestBody TenantRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Tenant tenant = tenantService.createTenant(req, user);
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable Long id,
            @RequestBody TenantRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Tenant tenant = tenantService.updateTenant(id, req, user);
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/{id}/move-out")
    public ResponseEntity<ApiResponse> moveOutTenant(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        tenantService.moveOutTenant(id, user);
        ApiResponse res = new ApiResponse();
        res.setMessage("Chuyen di thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        Tenant tenant = tenantService.findById(id, user);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Tenant> tenants = tenantService.findAll(user);
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Tenant>> getAvailableTenants(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Tenant> tenants = tenantService.findByTrangThai(TenantStatus.CHUA_NHAN_PHONG, user);
        return ResponseEntity.ok(tenants);
    }
}
