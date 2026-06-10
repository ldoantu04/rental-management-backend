package com.example.rental.service.impl;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import com.example.rental.dto.UserRequest;
import com.example.rental.model.User;
import com.example.rental.repository.UserRepository;
import com.example.rental.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final UserRepository userRepository;

    @Override
    public User createStaff(UserRequest req) throws Exception {
        User existUser = userRepository.findByEmail(req.getEmail());
        if (existUser != null) {
            throw new Exception("Email da ton tai trong he thong");
        }

        if (req.getSdt() != null) {
            User existPhone = userRepository.findBySdt(req.getSdt());
            if (existPhone != null) {
                throw new Exception("So dien thoai da ton tai trong he thong");
            }
        }

        User user = new User();
        user.setHoTen(req.getHoTen());
        user.setEmail(req.getEmail());
        user.setSdt(req.getSdt());
        user.setNgaySinh(req.getNgaySinh());
        user.setDiaChi(req.getDiaChi());
        user.setVaiTro(req.getVaiTro() != null ? req.getVaiTro() : UserRole.NHAN_VIEN);
        user.setPhamViQuanLy(req.getPhamViQuanLy());
        user.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : UserStatus.HOAT_DONG);
        user.setGhiChu(req.getGhiChu());
        user.setNgayTao(LocalDateTime.now());
        user.setNgaySua(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public User updateStaff(Long id, UserRequest req) throws Exception {
        User user = findStaffById(id);

        if (req.getHoTen() != null) {
            user.setHoTen(req.getHoTen());
        }
        if (req.getEmail() != null) {
            // Kiem tra email moi co bi trung khong
            User existUser = userRepository.findByEmail(req.getEmail());
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new Exception("Email da ton tai trong he thong");
            }
            user.setEmail(req.getEmail());
        }
        if (req.getSdt() != null) {
            User existPhone = userRepository.findBySdt(req.getSdt());
            if (existPhone != null && !existPhone.getId().equals(id)) {
                throw new Exception("So dien thoai da ton tai trong he thong");
            }
            user.setSdt(req.getSdt());
        }
        if (req.getNgaySinh() != null) {
            user.setNgaySinh(req.getNgaySinh());
        }
        if (req.getDiaChi() != null) {
            user.setDiaChi(req.getDiaChi());
        }
        if (req.getVaiTro() != null) {
            user.setVaiTro(req.getVaiTro());
        }
        if (req.getPhamViQuanLy() != null) {
            user.setPhamViQuanLy(req.getPhamViQuanLy());
        }
        if (req.getTrangThai() != null) {
            user.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            user.setGhiChu(req.getGhiChu());
        }
        user.setNgaySua(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public void deleteStaff(Long id) throws Exception {
        User user = findStaffById(id);

        // Kiem tra khong phai tai khoan chu so huu cuoi cung (SRS precondition)
        long ownerCount = userRepository.findAll().stream()
                .filter(u -> u.getVaiTro() == UserRole.QUAN_LY && u.getTrangThai() == UserStatus.HOAT_DONG)
                .count();
        if (user.getVaiTro() == UserRole.QUAN_LY && ownerCount <= 1) {
            throw new Exception("Khong the xoa tai khoan quan ly cuoi cung cua he thong");
        }

        userRepository.delete(user);
    }

    @Override
    public User findStaffById(Long id) throws Exception {
        return userRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay nhan vien voi id " + id));
    }

    @Override
    public List<User> findAllStaff() {
        return userRepository.findAllByOrderByNgayTaoDesc();
    }

    @Override
    public List<User> searchStaff(String keyword) {
        return userRepository.findByHoTenContainingIgnoreCaseOrEmailContainingIgnoreCaseOrSdtContaining(
                keyword, keyword, keyword);
    }
}
