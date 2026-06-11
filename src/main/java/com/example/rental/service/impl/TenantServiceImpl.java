package com.example.rental.service.impl;

import com.example.rental.domain.ContractStatus;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Tenant;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;

    @Override
    public Tenant createTenant(TenantRequest req) throws Exception {
        // Kiem tra CCCD bi trung (SRS 6b)
        if (req.getCccd() != null) {
            Tenant existTenant = tenantRepository.findByCccd(req.getCccd());
            if (existTenant != null) {
                throw new Exception("So CCCD da ton tai trong he thong. Vui long kiem tra lai");
            }
        }

        Tenant tenant = new Tenant();
        tenant.setHoTen(req.getHoTen());
        tenant.setNgaySinh(req.getNgaySinh());
        tenant.setGioiTinh(req.getGioiTinh());
        tenant.setCccd(req.getCccd());
        tenant.setSdt(req.getSdt());
        tenant.setEmail(req.getEmail());
        tenant.setDiaChi(req.getDiaChi());
        tenant.setHinhAnh(req.getHinhAnh());
        tenant.setGhiChu(req.getGhiChu());
        tenant.setNgayTao(LocalDateTime.now());
        tenant.setNgaySua(LocalDateTime.now());

        return tenantRepository.save(tenant);
    }

    @Override
    public Tenant updateTenant(Long id, TenantRequest req) throws Exception {
        Tenant tenant = findById(id);

        if (req.getHoTen() != null) {
            tenant.setHoTen(req.getHoTen());
        }
        if (req.getNgaySinh() != null) {
            tenant.setNgaySinh(req.getNgaySinh());
        }
        if (req.getGioiTinh() != null) {
            tenant.setGioiTinh(req.getGioiTinh());
        }
        if (req.getCccd() != null) {
            Tenant existTenant = tenantRepository.findByCccd(req.getCccd());
            if (existTenant != null && !existTenant.getId().equals(id)) {
                throw new Exception("So CCCD da ton tai trong he thong");
            }
            tenant.setCccd(req.getCccd());
        }
        if (req.getSdt() != null) {
            tenant.setSdt(req.getSdt());
        }
        if (req.getEmail() != null) {
            tenant.setEmail(req.getEmail());
        }
        if (req.getDiaChi() != null) {
            tenant.setDiaChi(req.getDiaChi());
        }
        if (req.getHinhAnh() != null) {
            tenant.setHinhAnh(req.getHinhAnh());
        }
        if (req.getGhiChu() != null) {
            tenant.setGhiChu(req.getGhiChu());
        }
        tenant.setNgaySua(LocalDateTime.now());

        return tenantRepository.save(tenant);
    }

    @Override
    public void deleteTenant(Long id) throws Exception {
        Tenant tenant = findById(id);

        List<Contract> activeContracts = contractRepository
                .findByKhachThueIdAndTrangThai(id, ContractStatus.DANG_HIEU_LUC);
        if (!activeContracts.isEmpty()) {
            throw new Exception("Khong the xoa khach thue khi con hop dong dang hieu luc");
        }

        tenantRepository.delete(tenant);
    }

    @Override
    public Tenant findById(Long id) throws Exception {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay khach thue voi id " + id));
    }

    @Override
    public List<Tenant> findAll() {
        return tenantRepository.findAllByOrderByNgayTaoDesc();
    }

    @Override
    public List<Tenant> search(String keyword) {
        return tenantRepository.search(keyword);
    }
}
