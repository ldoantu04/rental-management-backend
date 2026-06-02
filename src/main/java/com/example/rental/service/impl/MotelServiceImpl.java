package com.example.rental.service.impl;

import com.example.rental.dto.MotelRequest;
import com.example.rental.model.Motel;
import com.example.rental.model.User;
import com.example.rental.repository.MotelRepository;
import com.example.rental.service.MotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MotelServiceImpl implements MotelService {

    private final MotelRepository motelRepository;

    @Override
    public Motel createMotel(MotelRequest req, User nguoiTao) throws Exception {
        Motel existMotel = motelRepository.findByTenTro(req.getTenTro());
        if (existMotel != null) {
            throw new Exception("Ten nha tro da ton tai. Vui long kiem tra lai");
        }

        Motel motel = new Motel();
        motel.setTenTro(req.getTenTro());
        motel.setDiaChi(req.getDiaChi());
        motel.setSoTang(req.getSoTang());
        motel.setTongPhong(req.getTongPhong());
        motel.setTrangThai(req.getTrangThai());
        motel.setGhiChu(req.getGhiChu());
        motel.setNguoiTao(nguoiTao);
        motel.setNgayTao(LocalDateTime.now());
        motel.setNgaySua(LocalDateTime.now());

        return motelRepository.save(motel);
    }

    @Override
    public Motel updateMotel(Long id, MotelRequest req) throws Exception {
        Motel motel = findById(id);

        if (req.getTenTro() != null) {
            motel.setTenTro(req.getTenTro());
        }
        if (req.getDiaChi() != null) {
            motel.setDiaChi(req.getDiaChi());
        }
        if (req.getSoTang() != null) {
            motel.setSoTang(req.getSoTang());
        }
        if (req.getTongPhong() != null) {
            motel.setTongPhong(req.getTongPhong());
        }
        if (req.getTrangThai() != null) {
            motel.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            motel.setGhiChu(req.getGhiChu());
        }
        motel.setNgaySua(LocalDateTime.now());

        return motelRepository.save(motel);
    }

    @Override
    public void deleteMotel(Long id) throws Exception {

    }

    @Override
    public Motel findById(Long id) throws Exception {
        return motelRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay nha tro voi id " + id));
    }

    @Override
    public List<Motel> findAll() {
        return List.of();
    }

    @Override
    public List<Motel> search(String keyword) {
        return List.of();
    }
}
