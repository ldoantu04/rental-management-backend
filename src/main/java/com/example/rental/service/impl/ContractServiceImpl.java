package com.example.rental.service.impl;

import com.example.rental.dto.ContractRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.repository.ContractRepository;
import com.example.rental.service.ContractService;
import com.example.rental.service.RoomService;
import com.example.rental.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class
ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final TenantService tenantService;
    private final RoomService roomService;

    @Override
    public Contract createContract(ContractRequest req) throws Exception {
        Tenant tenant = tenantService.findById(req.getMaKhachThue());
        Room room = roomService.findById(req.getMaPhongTro());

        Contract contract = new Contract();
        String nextCode = "HD" + String.format("%03d", contractRepository.count() + 1);
        contract.setMaHopDong(nextCode);
        contract.setKhachThue(tenant);
        contract.setPhongTro(room);
        contract.setNgayBatDau(req.getNgayBatDau());
        contract.setNgayKetThuc(req.getNgayKetThuc());
        contract.setTienCoc(req.getTienCoc());
        contract.setGiaThue(req.getGiaThue());
        contract.setChuKyThanhToan(req.getChuKyThanhToan());
        contract.setDieuKhoan(req.getDieuKhoan());
        contract.setTrangThai(req.getTrangThai());
        contract.setNgayTao(LocalDateTime.now());
        contract.setNgaySua(LocalDateTime.now());

        return contractRepository.save(contract);
    }

    @Override
    public Contract updateContract(Long id, ContractRequest req) throws Exception {
        Contract contract = findById(id);

        if (req.getMaKhachThue() != null) {
            contract.setKhachThue(tenantService.findById(req.getMaKhachThue()));
        }
        if (req.getMaPhongTro() != null) {
            contract.setPhongTro(roomService.findById(req.getMaPhongTro()));
        }
        if (req.getNgayBatDau() != null) contract.setNgayBatDau(req.getNgayBatDau());
        if (req.getNgayKetThuc() != null) contract.setNgayKetThuc(req.getNgayKetThuc());
        if (req.getTienCoc() != null) contract.setTienCoc(req.getTienCoc());
        if (req.getGiaThue() != null) contract.setGiaThue(req.getGiaThue());
        if (req.getChuKyThanhToan() != null) contract.setChuKyThanhToan(req.getChuKyThanhToan());
        if (req.getDieuKhoan() != null) contract.setDieuKhoan(req.getDieuKhoan());
        if (req.getTrangThai() != null) contract.setTrangThai(req.getTrangThai());
        if (req.getLyDoHuy() != null) contract.setLyDoHuy(req.getLyDoHuy());
        contract.setNgaySua(LocalDateTime.now());

        return contractRepository.save(contract);
    }

    @Override
    public void deleteContract(Long id) throws Exception {
        Contract contract = findById(id);
        contractRepository.delete(contract);
    }

    @Override
    public Contract findById(Long id) throws Exception {
        return contractRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay hop dong voi id: " + id));
    }

    @Override
    public List<Contract> findAll() {
        return contractRepository.findAll();
    }
}
