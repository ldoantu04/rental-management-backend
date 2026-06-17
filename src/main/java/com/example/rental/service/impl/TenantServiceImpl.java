package com.example.rental.service.impl;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.domain.TenantStatus;
import com.example.rental.dto.RoommateRequest;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Roommate;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public Tenant createTenant(TenantRequest req) throws Exception {
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
        tenant.setAnhGiayTo(req.getAnhGiayTo() != null ? req.getAnhGiayTo() : new ArrayList<>());

        tenant.setTrangThai(TenantStatus.CHUA_NHAN_PHONG);
        tenant.setGhiChu(req.getGhiChu());
        tenant.setNgayTao(LocalDateTime.now());
        tenant.setNgaySua(LocalDateTime.now());

        Tenant savedTenant = tenantRepository.save(tenant);
        attachNguoiOCung(savedTenant, req.getDanhSachNguoiOCung());
        return savedTenant;
    }

    @Override
    @Transactional
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
        if (req.getAnhGiayTo() != null) {
            tenant.setAnhGiayTo(req.getAnhGiayTo());
        }
        if (req.getDanhSachNguoiOCung() != null) {
            tenant.getDanhSachNguoiOCung().clear();
            attachNguoiOCung(tenant, req.getDanhSachNguoiOCung());
        }
        if (req.getGhiChu() != null) {
            tenant.setGhiChu(req.getGhiChu());
        }
        tenant.setNgaySua(LocalDateTime.now());

        return tenantRepository.save(tenant);
    }

    private void attachNguoiOCung(Tenant tenant, List<RoommateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (RoommateRequest req : requests) {
            if (req.getHoTen() == null || req.getHoTen().trim().isEmpty()) {
                continue;
            }
            Roommate roommate = new Roommate();
            roommate.setKhachThue(tenant);
            roommate.setHoTen(req.getHoTen());
            roommate.setQuanHe(req.getQuanHe());
            roommate.setCccd(req.getCccd());
            roommate.setSdt(req.getSdt());
            roommate.setNgayTao(LocalDateTime.now());
            tenant.getDanhSachNguoiOCung().add(roommate);
        }
    }

    @Override
    @Transactional
    public void moveOutTenant(Long id) throws Exception {
        Tenant tenant = findById(id);

        if (tenant.getTrangThai() == TenantStatus.DA_CHUYEN_DI) {
            throw new Exception("Khach thue da o trang thai da chuyen di");
        }

        // Hủy các hợp đồng còn hạn của khách thuê
        List<Contract> activeContracts = contractRepository
                .findByKhachThueIdAndTrangThai(id, ContractStatus.DANG_HIEU_LUC);
        for (Contract contract : activeContracts) {
            contract.setTrangThai(ContractStatus.DA_HUY);
            contract.setLyDoHuy("Khách trả phòng trước hạn");
            contract.setNgayHuy(LocalDate.now());
            contract.setNgaySua(LocalDateTime.now());
            contractRepository.save(contract);
        }

        // Giải phóng phòng trọ
        if (tenant.getPhongTro() != null) {
            Room room = tenant.getPhongTro();
            room.setTrangThai(RoomStatus.TRONG);
            room.setNgaySua(LocalDateTime.now());
            roomRepository.save(room);
            tenant.setPhongTro(null);
        }

        // Cập nhật trạng thái khách thuê
        tenant.setTrangThai(TenantStatus.DA_CHUYEN_DI);
        tenant.setNgayBatDauThue(null);
        tenant.setTienCoc(null);
        tenant.setNgaySua(LocalDateTime.now());
        tenantRepository.save(tenant);
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
    public List<Tenant> findByTrangThai(TenantStatus trangThai) {
        return tenantRepository.findByTrangThaiOrderByNgayTaoDesc(trangThai);
    }
}
