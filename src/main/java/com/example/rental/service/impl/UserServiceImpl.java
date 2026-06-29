package com.example.rental.service.impl;

import com.example.rental.config.JwtProvider;
import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import com.example.rental.dto.EmployeeRequest;
import com.example.rental.dto.EmployeeResponse;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.repository.UserRepository;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MotelRepository motelRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final JwtProvider jwtProvider;

    @Override
    public User findByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("Khong tim thay nguoi dung voi email " + email);
        }
        return user;
    }

    @Override
    public User findByJwt(String jwt) throws Exception {
        String email = jwtProvider.getEmailFromJwtToken(jwt);
        return this.findByEmail(email);
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return userRepository.findAllByOrderByNgayTaoDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmployeeResponse> searchEmployees(String keyword, UserRole vaiTro, UserStatus trangThai) {
        return userRepository.searchEmployees(keyword, vaiTro, trangThai).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay nhan vien voi id " + id));
        return toResponse(user);
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest req, User currentUser) throws Exception {
        if (currentUser != null && currentUser.getVaiTro() == UserRole.NHAN_VIEN) {
            throw new Exception("Ban khong co quyen tao nhan vien");
        }

        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new Exception("Email khong duoc de trong");
        }
        if (userRepository.existsByEmail(req.getEmail().trim())) {
            throw new Exception("Email da ton tai trong he thong");
        }
        if (req.getSdt() != null && !req.getSdt().trim().isEmpty()) {
            if (userRepository.existsBySdt(req.getSdt().trim())) {
                throw new Exception("So dien thoai da ton tai trong he thong");
            }
        }
        if (req.getCccd() != null && !req.getCccd().trim().isEmpty()) {
            if (userRepository.existsByCccd(req.getCccd().trim())) {
                throw new Exception("So CCCD da ton tai trong he thong");
            }
        }

        if (req.getVaiTro() == null) {
            throw new Exception("Vai tro khong duoc de trong");
        }

        if (!isAdminLevel(req.getVaiTro()) && (req.getAssignedMotelIds() == null || req.getAssignedMotelIds().isEmpty())) {
            throw new Exception("Nhan vien phai duoc gan it nhat mot nha tro");
        }

        User user = new User();
        user.setHoTen(req.getHoTen());
        user.setEmail(req.getEmail().trim());
        user.setSdt(req.getSdt());
        user.setNgaySinh(req.getNgaySinh());
        user.setDiaChi(req.getDiaChi());
        user.setCccd(req.getCccd());
        user.setNgayVaoLam(req.getNgayVaoLam() != null ? req.getNgayVaoLam() : java.time.LocalDate.now());
        user.setVaiTro(req.getVaiTro());
        user.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : UserStatus.HOAT_DONG);
        user.setGhiChu(req.getGhiChu());
        user.setNgayTao(LocalDateTime.now());
        user.setNgaySua(LocalDateTime.now());

        if (!isAdminLevel(req.getVaiTro()) && req.getAssignedMotelIds() != null) {
            for (Long motelId : req.getAssignedMotelIds()) {
                Motel motel = motelRepository.findById(motelId)
                        .orElseThrow(() -> new Exception("Khong tim thay nha tro voi id " + motelId));
                user.getAssignedMotels().add(motel);
            }
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest req, User currentUser) throws Exception {
        if (currentUser != null && currentUser.getVaiTro() == UserRole.NHAN_VIEN) {
            throw new Exception("Ban khong co quyen cap nhat nhan vien");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay nhan vien voi id " + id));


        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmailAndIdNot(req.getEmail().trim(), id)) {
                throw new Exception("Email da ton tai trong he thong");
            }
            user.setEmail(req.getEmail().trim());
        }

        if (req.getSdt() != null && !req.getSdt().trim().isEmpty()) {
            if (userRepository.existsBySdtAndIdNot(req.getSdt().trim(), id)) {
                throw new Exception("So dien thoai da ton tai trong he thong");
            }
            user.setSdt(req.getSdt().trim());
        }

        if (req.getHoTen() != null) {
            user.setHoTen(req.getHoTen());
        }
        if (req.getNgaySinh() != null) {
            user.setNgaySinh(req.getNgaySinh());
        }
        if (req.getDiaChi() != null) {
            user.setDiaChi(req.getDiaChi());
        }
        if (req.getCccd() != null && !req.getCccd().trim().isEmpty()) {
            if (userRepository.existsByCccdAndIdNot(req.getCccd().trim(), id)) {
                throw new Exception("So CCCD da ton tai trong he thong");
            }
            user.setCccd(req.getCccd().trim());
        } else if (req.getCccd() != null && req.getCccd().trim().isEmpty()) {
            user.setCccd(null);
        }
        if (req.getNgayVaoLam() != null) {
            user.setNgayVaoLam(req.getNgayVaoLam());
        } else if (user.getNgayVaoLam() == null) {
            user.setNgayVaoLam(java.time.LocalDate.now());
        }
        if (req.getTrangThai() != null) {
            user.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            user.setGhiChu(req.getGhiChu());
        }

        if (req.getVaiTro() != null && req.getVaiTro() != user.getVaiTro()) {
            UserRole newRole = req.getVaiTro();
            if (!isAdminLevel(newRole) && (req.getAssignedMotelIds() == null || req.getAssignedMotelIds().isEmpty())) {
                throw new Exception("Nhan vien phai duoc gan it nhat mot nha tro");
            }
            user.setVaiTro(newRole);
        }

        if (!isAdminLevel(req.getVaiTro()) || (req.getVaiTro() == null && !isAdminLevel(user))) {
            if (req.getAssignedMotelIds() != null) {
                user.getAssignedMotels().clear();
                for (Long motelId : req.getAssignedMotelIds()) {
                    Motel motel = motelRepository.findById(motelId)
                            .orElseThrow(() -> new Exception("Khong tim thay nha tro voi id " + motelId));
                    user.getAssignedMotels().add(motel);
                }
            }
        }

        user.setNgaySua(LocalDateTime.now());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public void deleteEmployee(Long id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay nhan vien voi id " + id));
        userRepository.delete(user);
    }

    @Override
    public List<EmployeeResponse.MotelSimple> getAvailableMotelsForEmployee(Long employeeId) throws Exception {
        return motelRepository.findAll().stream()
                .map(EmployeeResponse::fromMotel)
                .collect(Collectors.toList());
    }

    private boolean isAdminLevel(UserRole role) {
        return role == UserRole.QUAN_LY;
    }

    private boolean isAdminLevel(User user) {
        return user != null && isAdminLevel(user.getVaiTro());
    }

    @Override
    public Set<Long> getAssignedMotelIds(User user) {
        if (user == null) return new HashSet<>();
        if (isAdminLevel(user)) {
            return motelRepository.findAll().stream()
                    .map(Motel::getId)
                    .collect(Collectors.toSet());
        }
        return user.getAssignedMotels().stream()
                .map(Motel::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAdmin(User user) {
        return isAdminLevel(user);
    }

    @Override
    public boolean canAccessMotel(User user, Long motelId) {
        if (isAdmin(user)) return true;
        if (motelId == null) return false;
        Set<Long> allowed = getAssignedMotelIds(user);
        return allowed.contains(motelId);
    }

    @Override
    public boolean canAccessRoom(User user, Long roomId) {
        if (isAdmin(user)) return true;
        if (roomId == null) return false;
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getNhaTro() == null) return false;
        return canAccessMotel(user, room.getNhaTro().getId());
    }

    @Override
    public boolean canAccessContract(User user, Long contractId) {
        if (isAdmin(user)) return true;
        if (contractId == null) return false;
        Contract contract = contractRepository.findById(contractId).orElse(null);
        if (contract == null || contract.getPhongTro() == null) return false;
        return canAccessRoom(user, contract.getPhongTro().getId());
    }

    @Override
    public boolean canAccessInvoice(User user, Long invoiceId) {
        if (isAdmin(user)) return true;
        if (invoiceId == null) return false;
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null || invoice.getHopDong() == null) return false;
        return canAccessContract(user, invoice.getHopDong().getId());
    }

    @Override
    public boolean canAccessTenant(User user, Long tenantId) {
        if (isAdmin(user)) return true;
        if (tenantId == null) return false;
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return false;
        if (tenant.getPhongTro() == null) {
            return true;
        }
        return canAccessRoom(user, tenant.getPhongTro().getId());
    }

    private EmployeeResponse toResponse(User user) {
        if (user == null) return null;
        EmployeeResponse r = new EmployeeResponse();
        r.setId(user.getId());
        r.setHoTen(user.getHoTen());
        r.setEmail(user.getEmail());
        r.setSdt(user.getSdt());
        r.setNgaySinh(user.getNgaySinh());
        r.setDiaChi(user.getDiaChi());
        r.setCccd(user.getCccd());
        r.setNgayVaoLam(user.getNgayVaoLam());
        r.setVaiTro(user.getVaiTro());
        r.setTrangThai(user.getTrangThai());
        r.setGhiChu(user.getGhiChu());
        r.setNgayTao(user.getNgayTao());
        r.setNgaySua(user.getNgaySua());
        r.setAssignedMotels(user.getAssignedMotels().stream()
                .map(EmployeeResponse::fromMotel)
                .collect(Collectors.toSet()));
        return r;
    }
}
