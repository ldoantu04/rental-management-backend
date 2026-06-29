package com.example.rental.service.impl;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.domain.TenantStatus;
import com.example.rental.dto.ContractRequest;
import com.example.rental.dto.ContractServiceItemRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.ContractServiceItem;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.ContractServiceItemRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.service.ContractService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final ContractServiceItemRepository contractServiceItemRepository;
    private final UserService userService;

    @Override
    @Transactional
    public Contract createContract(ContractRequest req) throws Exception {
        return createContract(req, null);
    }

    @Override
    @Transactional
    public Contract createContract(ContractRequest req, User nguoiTao) throws Exception {
        Tenant tenant = tenantRepository.findById(req.getMaKhachThue())
                .orElseThrow(() -> new Exception("Khong tim thay khach thue voi id " + req.getMaKhachThue()));

        if (tenant.getTrangThai() != TenantStatus.CHUA_NHAN_PHONG) {
            throw new Exception("Khach thue khong o trang thai cho nhan phong, khong the tao hop dong");
        }

        Room room = roomRepository.findById(req.getMaPhongTro())
                .orElseThrow(() -> new Exception("Khong tim thay phong tro voi id " + req.getMaPhongTro()));

        if (room.getTrangThai() != RoomStatus.TRONG) {
            throw new Exception("Phong tro dang co nguoi thue hoac khong o trang thai trong, khong the tao hop dong");
        }

        List<Contract> activeContracts = contractRepository.findByPhongTroIdAndTrangThai(room.getId(), ContractStatus.DANG_HIEU_LUC);
        if (activeContracts != null && !activeContracts.isEmpty()) {
            throw new Exception("Phong tro dang co hop dong hieu luc, khong the tao hop dong moi");
        }

        long count = contractRepository.count();
        String maHopDong = "HD" + String.format("%03d", count + 1);

        Contract contract = new Contract();
        contract.setMaHopDong(maHopDong);
        contract.setKhachThue(tenant);
        contract.setPhongTro(room);
        contract.setNgayBatDau(req.getNgayBatDau());
        contract.setNgayKetThuc(req.getNgayKetThuc());
        contract.setTienCoc(req.getTienCoc());
        contract.setGiaThue(req.getGiaThue());
        contract.setGiaDien(req.getGiaDien());
        contract.setGiaNuoc(req.getGiaNuoc());
        contract.setKieuTinhNuoc(req.getKieuTinhNuoc());
        contract.setChuKyThanhToan(req.getChuKyThanhToan() != null ? req.getChuKyThanhToan() : 5);
        contract.setNgayThanhToan(req.getNgayThanhToan());
        contract.setDieuKhoan(req.getDieuKhoan());
        contract.setFileHopDong(req.getFileHopDong());
        contract.setTrangThai(ContractStatus.DANG_HIEU_LUC);
        contract.setNguoiTao(nguoiTao);
        contract.setNgayTao(LocalDateTime.now());
        contract.setNgaySua(LocalDateTime.now());

        Contract saved = contractRepository.save(contract);
        attachDichVu(saved, req.getDanhSachDichVu());
        syncTenantAndRoom(saved);
        return saved;
    }

    @Override
    @Transactional
    public Contract updateContract(Long id, ContractRequest req) throws Exception {
        return updateContract(id, req, null);
    }

    @Override
    @Transactional
    public Contract updateContract(Long id, ContractRequest req, User nguoiSua) throws Exception {
        Contract contract = findById(id);

        if (req.getMaKhachThue() != null) {
            Tenant tenant = tenantRepository.findById(req.getMaKhachThue())
                    .orElseThrow(() -> new Exception("Khong tim thay khach thue voi id " + req.getMaKhachThue()));
            contract.setKhachThue(tenant);
        }
        if (req.getMaPhongTro() != null) {
            Room newRoom = roomRepository.findById(req.getMaPhongTro())
                    .orElseThrow(() -> new Exception("Khong tim thay phong tro voi id " + req.getMaPhongTro()));
            if (contract.getPhongTro() != null && !contract.getPhongTro().getId().equals(newRoom.getId())) {
                Room oldRoom = contract.getPhongTro();
                if (oldRoom.getTrangThai() == RoomStatus.DANG_THUE) {
                    oldRoom.setTrangThai(RoomStatus.TRONG);
                    roomRepository.save(oldRoom);
                }
                if (newRoom.getTrangThai() != RoomStatus.TRONG) {
                    throw new Exception("Phong tro moi khong o trang thai trong, khong the chuyen hop dong");
                }
                List<Contract> otherActive = contractRepository.findByPhongTroIdAndTrangThai(newRoom.getId(), ContractStatus.DANG_HIEU_LUC);
                boolean hasOtherActive = otherActive != null && otherActive.stream().anyMatch(c -> !c.getId().equals(contract.getId()));
                if (hasOtherActive) {
                    throw new Exception("Phong tro moi dang co hop dong hieu luc khac, khong the chuyen hop dong");
                }
            }
            contract.setPhongTro(newRoom);
        }
        if (req.getNgayBatDau() != null) {
            contract.setNgayBatDau(req.getNgayBatDau());
        }
        if (req.getNgayKetThuc() != null) {
            contract.setNgayKetThuc(req.getNgayKetThuc());
        }
        if (req.getTienCoc() != null) {
            contract.setTienCoc(req.getTienCoc());
        }
        if (req.getGiaThue() != null) {
            contract.setGiaThue(req.getGiaThue());
        }
        if (req.getGiaDien() != null) {
            contract.setGiaDien(req.getGiaDien());
        }
        if (req.getGiaNuoc() != null) {
            contract.setGiaNuoc(req.getGiaNuoc());
        }
        if (req.getKieuTinhNuoc() != null) {
            contract.setKieuTinhNuoc(req.getKieuTinhNuoc());
        }
        if (req.getChuKyThanhToan() != null) {
            contract.setChuKyThanhToan(req.getChuKyThanhToan());
        }
        if (req.getNgayThanhToan() != null) {
            contract.setNgayThanhToan(req.getNgayThanhToan());
        }
        if (req.getDieuKhoan() != null) {
            contract.setDieuKhoan(req.getDieuKhoan());
        }
        if (req.getFileHopDong() != null) {
            contract.setFileHopDong(req.getFileHopDong());
        }

        ContractStatus targetStatus = null;
        if (req.getTrangThai() != null) {
            contract.setTrangThai(req.getTrangThai());
            targetStatus = req.getTrangThai();
        }
        if (req.getLyDoHuy() != null) {
            contract.setLyDoHuy(req.getLyDoHuy());
            if (req.getTrangThai() == ContractStatus.DA_HUY) {
                contract.setNgayHuy(LocalDate.now());
            }
        }
        contract.setNgaySua(LocalDateTime.now());

        Contract saved = contractRepository.save(contract);
        if (req.getDanhSachDichVu() != null) {
            contractServiceItemRepository.deleteByHopDongId(id);
            attachDichVu(saved, req.getDanhSachDichVu());
        }
        if (targetStatus != null || req.getNgayKetThuc() != null || req.getMaKhachThue() != null || req.getMaPhongTro() != null) {
            syncTenantAndRoom(saved);
        }
        return saved;
    }

    @Transactional
    public void cancelContract(Long id, String lyDoHuy) throws Exception {
        cancelContract(id, lyDoHuy, null);
    }

    @Override
    @Transactional
    public void cancelContract(Long id, String lyDoHuy, User currentUser) throws Exception {
        Contract contract = findById(id, currentUser);
        contract.setTrangThai(ContractStatus.DA_HUY);
        if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
            contract.setLyDoHuy(lyDoHuy);
        }
        contract.setNgayHuy(LocalDate.now());
        contract.setNgaySua(LocalDateTime.now());
        Contract saved = contractRepository.save(contract);
        syncTenantAndRoom(saved);
    }

    private void attachDichVu(Contract contract, List<ContractServiceItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (ContractServiceItemRequest req : requests) {
            if (req.getTenDichVu() == null || req.getTenDichVu().trim().isEmpty()) {
                continue;
            }
            ContractServiceItem item = new ContractServiceItem();
            item.setHopDong(contract);
            item.setTenDichVu(req.getTenDichVu());
            item.setKieuTinh(req.getKieuTinh());
            item.setDonGia(req.getDonGia() != null ? req.getDonGia() : java.math.BigDecimal.ZERO);
            item.setLaDichVuBoSung(true);
            contractServiceItemRepository.save(item);
            contract.getDanhSachDichVu().add(item);
        }
    }

    private void syncTenantAndRoom(Contract contract) {
        Room room = contract.getPhongTro();
        Tenant tenant = contract.getKhachThue();

        if (room == null) {
            return;
        }

        switch (contract.getTrangThai()) {
            case DANG_HIEU_LUC -> {
                room.setTrangThai(RoomStatus.DANG_THUE);
                if (tenant != null) {
                    tenant.setPhongTro(room);
                    tenant.setNgayBatDauThue(contract.getNgayBatDau());
                    tenant.setTienCoc(contract.getTienCoc());
                    tenant.setTrangThai(TenantStatus.DANG_THUE);
                    tenant.setNgaySua(LocalDateTime.now());
                    tenantRepository.save(tenant);
                }
            }
            case DA_HUY, DA_HET_HAN -> {
                releaseRoom(room);
                if (tenant != null) {
                    tenant.setTrangThai(TenantStatus.CHUA_NHAN_PHONG);
                    tenant.setPhongTro(null);
                    tenant.setNgayBatDauThue(null);
                    tenant.setTienCoc(null);
                    tenant.setNgaySua(LocalDateTime.now());
                    tenantRepository.save(tenant);
                }
            }
            default -> {
            }
        }
        room.setNgaySua(LocalDateTime.now());
        roomRepository.save(room);
    }

    private void releaseRoom(Room room) {
        List<Contract> active = contractRepository.findByPhongTroIdAndTrangThaiNot(room.getId(), ContractStatus.DA_HUY);
        boolean stillActive = active.stream()
                .anyMatch(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC);
        if (!stillActive) {
            room.setTrangThai(RoomStatus.TRONG);
            roomRepository.save(room);
        }
    }

    @Override
    public Contract findById(Long id) throws Exception {
        return findById(id, null);
    }

    @Override
    public Contract findById(Long id, User currentUser) throws Exception {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay hop dong voi id " + id));
        contract.getDanhSachDichVu().size();
        if (contract.getKhachThue() != null && contract.getKhachThue().getDanhSachNguoiOCung() != null) {
            contract.getKhachThue().getDanhSachNguoiOCung().size();
        }
        if (currentUser != null && !userService.isAdmin(currentUser)) {
            if (!userService.canAccessContract(currentUser, id)) {
                throw new Exception("Ban khong co quyen truy cap hop dong nay");
            }
        }
        return contract;
    }

    @Override
    public List<Contract> findAll() {
        return contractRepository.findAllByOrderByNgayTaoDesc();
    }

    @Override
    public List<Contract> findAll(User currentUser) {
        if (currentUser == null || userService.isAdmin(currentUser)) {
            return findAll();
        }
        Set<Long> allowedMotelIds = userService.getAssignedMotelIds(currentUser);
        return contractRepository.findAllByOrderByNgayTaoDesc().stream()
                .filter(c -> c.getPhongTro() != null
                        && c.getPhongTro().getNhaTro() != null
                        && allowedMotelIds.contains(c.getPhongTro().getNhaTro().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Contract> findByTrangThai(ContractStatus trangThai) {
        return findByTrangThai(trangThai, null);
    }

    @Override
    public List<Contract> findByTrangThai(ContractStatus trangThai, User currentUser) {
        return findAll(currentUser).stream()
                .filter(c -> trangThai == null || c.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }

    @Override
    public List<Contract> findByPhongTroIdAndTrangThai(Long phongTroId, ContractStatus trangThai) {
        return contractRepository.findByPhongTroIdAndTrangThai(phongTroId, trangThai);
    }
}
