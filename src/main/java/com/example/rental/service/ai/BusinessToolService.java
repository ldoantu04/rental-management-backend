package com.example.rental.service.ai;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.UserRole;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.UserRepository;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessToolService {

    private final ResolverService resolver;
    private final ContractRepository contractRepository;
    private final InvoiceService invoiceService;
    private final UserService userService;
    private final UserRepository userRepository;

    public Map<String, Object> getRoomOverview(JsonNode args, User user) throws Exception {
        String roomCode = text(args, "maPhong");
        String motelName = text(args, "tenNhaTro");
        Room room = resolver.resolveRoom(roomCode, motelName);
        requireAccessRoom(user, room);
        Contract contract = null;
        try {
            contract = resolver.resolveCurrentContract(room);
        } catch (Exception ignored) {
        }
        Tenant tenant = contract != null ? contract.getKhachThue() : null;
        Invoice latestInvoice = contract != null ? resolver.findLatestInvoice(contract) : null;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phong", room.getMaPhong());
        data.put("nhaTro", room.getNhaTro() != null ? room.getNhaTro().getTenTro() : "N/A");
        data.put("trangThaiPhong", room.getTrangThai());
        data.put("giaThue", room.getGiaThue());
        if (contract != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("maHopDong", contract.getMaHopDong());
            c.put("ngayBatDau", contract.getNgayBatDau());
            c.put("ngayKetThuc", contract.getNgayKetThuc());
            c.put("trangThai", contract.getTrangThaiHienThi());
            data.put("hopDong", c);
            if (tenant != null) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("hoTen", tenant.getHoTen());
                t.put("sdt", tenant.getSdt());
                t.put("cccd", tenant.getCccd());
                data.put("khachThue", t);
            }
        } else {
            data.put("hopDong", "Khong co hop dong hieu luc");
        }
        if (latestInvoice != null) {
            Map<String, Object> inv = new LinkedHashMap<>();
            inv.put("maHoaDon", latestInvoice.getMaHoaDon());
            inv.put("kyHoaDon", latestInvoice.getKyHoaDon());
            inv.put("tongTien", latestInvoice.getTongTien());
            inv.put("trangThai", latestInvoice.getTrangThai());
            data.put("hoaDonGanNhat", inv);
        }
        return data;
    }

    public Map<String, Object> getTenantOverview(JsonNode args, User user) throws Exception {
        Tenant tenant = resolver.resolveTenant(
                text(args, "hoTen"),
                text(args, "sdt"),
                text(args, "cccd"));
        List<Contract> contracts = new ArrayList<>(
                contractRepository.findByKhachThueIdAndTrangThai(tenant.getId(), ContractStatus.DANG_HIEU_LUC));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hoTen", tenant.getHoTen());
        data.put("sdt", tenant.getSdt());
        data.put("cccd", tenant.getCccd());
        data.put("trangThai", tenant.getTrangThai());
        if (!contracts.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Contract c : contracts) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("maHopDong", c.getMaHopDong());
                e.put("phong", c.getPhongTro() != null ? c.getPhongTro().getMaPhong() : "N/A");
                e.put("ngayBatDau", c.getNgayBatDau());
                e.put("ngayKetThuc", c.getNgayKetThuc());
                list.add(e);
            }
            data.put("hopDongHieuLuc", list);
        } else {
            data.put("hopDongHieuLuc", "Khong co");
        }
        return data;
    }

    public Map<String, Object> checkRoomStatus(JsonNode args, User user) throws Exception {
        Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
        requireAccessRoom(user, room);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phong", room.getMaPhong());
        data.put("nhaTro", room.getNhaTro() != null ? room.getNhaTro().getTenTro() : "N/A");
        data.put("trangThai", room.getTrangThai());
        data.put("giaThue", room.getGiaThue());
        data.put("dienTich", room.getDienTich());
        data.put("soNguoi", room.getSoNguoi());
        return data;
    }

    public Map<String, Object> getActiveTenants(JsonNode args, User user) throws Exception {
        String motelName = text(args, "tenNhaTro");
        Long motelIdFilter = null;
        if (motelName != null && !motelName.isBlank()) {
            motelIdFilter = resolver.resolveMotel(motelName).getId();
        }

        List<Contract> all = contractRepository.findAll().stream()
                .filter(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC)
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Contract c : all) {
            Room r = c.getPhongTro();
            if (r == null) continue;
            if (motelIdFilter != null && (r.getNhaTro() == null
                    || !motelIdFilter.equals(r.getNhaTro().getId()))) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("maHopDong", c.getMaHopDong());
            entry.put("phong", r.getMaPhong());
            entry.put("nhaTro", r.getNhaTro() != null ? r.getNhaTro().getTenTro() : null);
            if (c.getKhachThue() != null) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("hoTen", c.getKhachThue().getHoTen());
                t.put("sdt", c.getKhachThue().getSdt());
                t.put("cccd", c.getKhachThue().getCccd());
                entry.put("khachThue", t);
            }
            entry.put("ngayBatDau", c.getNgayBatDau());
            entry.put("ngayKetThuc", c.getNgayKetThuc());
            result.add(entry);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tongSoKhach", result.size());
        data.put("danhSach", result);
        return data;
    }

    public Map<String, Object> getContractsOfRoom(JsonNode args, User user) throws Exception {
        Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
        List<Contract> list = resolver.resolveContractsOfRoom(room);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Contract c : list) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("maHopDong", c.getMaHopDong());
            e.put("trangThai", c.getTrangThai() != null ? c.getTrangThai().name() : null);
            e.put("trangThaiHienThi", c.getTrangThaiHienThi());
            e.put("ngayBatDau", c.getNgayBatDau());
            e.put("ngayKetThuc", c.getNgayKetThuc());
            if (c.getKhachThue() != null) {
                e.put("khach", c.getKhachThue().getHoTen());
            }
            e.put("giaThue", c.getGiaThue());
            result.add(e);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phong", room.getMaPhong());
        data.put("nhaTro", room.getNhaTro() != null ? room.getNhaTro().getTenTro() : null);
        data.put("tongHopDong", result.size());
        data.put("danhSach", result);
        return data;
    }

    public Map<String, Object> getRoomOfTenant(JsonNode args, User user) throws Exception {
        Tenant tenant = resolver.resolveTenant(
                text(args, "hoTen"),
                text(args, "sdt"),
                text(args, "cccd"));
        Room room = resolver.resolveRoomByTenant(tenant);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hoTen", tenant.getHoTen());
        if (room != null) {
            data.put("phong", room.getMaPhong());
            data.put("nhaTro", room.getNhaTro() != null ? room.getNhaTro().getTenTro() : null);
            data.put("trangThaiPhong", room.getTrangThai());
            Contract current = null;
            try { current = resolver.resolveCurrentContract(room); } catch (Exception ignored) {}
            if (current != null) {
                data.put("maHopDong", current.getMaHopDong());
                data.put("ngayBatDau", current.getNgayBatDau());
                data.put("ngayKetThuc", current.getNgayKetThuc());
            }
        } else {
            data.put("phong", "Khong thue phong nao");
        }
        return data;
    }

    public Map<String, Object> getInvoiceOverview(JsonNode args, User user) throws Exception {
        Invoice invoice;
        if (args.hasNonNull("maHoaDon")) {
            invoice = resolver.resolveInvoice(args.get("maHoaDon").asText());
        } else {
            Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
            Contract contract = resolver.resolveCurrentContract(room);
            invoice = resolver.findLatestInvoice(contract);
        }
        if (invoice == null) {
            throw new IllegalArgumentException("Khong tim thay hoa don");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHoaDon", invoice.getMaHoaDon());
        data.put("kyHoaDon", invoice.getKyHoaDon());
        data.put("hanThanhToan", invoice.getHanThanhToan());
        data.put("chiSoDienCu", invoice.getChiSoDienCu());
        data.put("chiSoDienMoi", invoice.getChiSoDienMoi());
        data.put("chiSoNuocCu", invoice.getChiSoNuocCu());
        data.put("chiSoNuocMoi", invoice.getChiSoNuocMoi());
        data.put("tongTien", invoice.getTongTien());
        data.put("trangThai", invoice.getTrangThai());
        if (invoice.getHopDong() != null) {
            data.put("maHopDong", invoice.getHopDong().getMaHopDong());
            if (invoice.getHopDong().getPhongTro() != null) {
                data.put("phong", invoice.getHopDong().getPhongTro().getMaPhong());
            }
            if (invoice.getHopDong().getKhachThue() != null) {
                data.put("khach", invoice.getHopDong().getKhachThue().getHoTen());
            }
        }
        return data;
    }

    public Map<String, Object> calculateInvoice(JsonNode args, User user) throws Exception {
        Contract contract;
        if (args.hasNonNull("maHopDong")) {
            contract = resolver.resolveContractByCode(args.get("maHopDong").asText());
        } else {
            Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
            contract = resolver.resolveCurrentContract(room);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("giaThue", contract.getGiaThue() != null ? contract.getGiaThue() : contract.getPhongTro().getGiaThue());
        return data;
    }

    private void requireAccessRoom(User user, Room room) throws Exception {
        if (room == null) return;
        if (room.getNhaTro() == null) return;
        if (user == null) return;
        if (user.getVaiTro() == UserRole.QUAN_LY) return;
        if (!userService.canAccessRoom(user, room.getId())) {
            throw new IllegalArgumentException(
                    "Ban khong co quyen truy cap phong " + room.getMaPhong() +
                    " thuoc nha tro " + (room.getNhaTro() != null ? room.getNhaTro().getTenTro() : ""));
        }
    }

    public Map<String, Object> getEmployeeOverview(JsonNode args, User user) throws Exception {
        if (user == null || user.getVaiTro() != UserRole.QUAN_LY) {
            throw new IllegalArgumentException("Chi quan ly moi co quyen tra cuu thong tin nhan vien.");
        }
        String keyword = text(args, "tuKhoa");
        List<User> employees = userRepository.searchEmployees(keyword, UserRole.NHAN_VIEN, null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tongSoNhanVien", employees.size());
        List<Map<String, Object>> list = new ArrayList<>();
        for (User emp : employees) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("hoTen", emp.getHoTen());
            e.put("email", emp.getEmail());
            e.put("sdt", emp.getSdt());
            e.put("vaiTro", emp.getVaiTro());
            e.put("trangThai", emp.getTrangThai());
            if (emp.getAssignedMotels() != null && !emp.getAssignedMotels().isEmpty()) {
                List<String> motelNames = emp.getAssignedMotels().stream()
                        .map(m -> m.getTenTro())
                        .toList();
                e.put("nhaTroPhuTrach", motelNames);
            }
            list.add(e);
        }
        data.put("danhSach", list);
        return data;
    }

    public Map<String, Object> getUserOverview(JsonNode args, User user) throws Exception {
        if (user == null || user.getVaiTro() != UserRole.QUAN_LY) {
            throw new IllegalArgumentException("Chi quan ly moi co quyen tra cuu danh sach tai khoan.");
        }
        String keyword = text(args, "tuKhoa");
        String vaiTroStr = text(args, "vaiTro");
        UserRole vaiTro = null;
        if (vaiTroStr != null && !vaiTroStr.isBlank()) {
            try {
                vaiTro = UserRole.valueOf(vaiTroStr.toUpperCase());
            } catch (Exception ignored) {}
        }
        List<User> users = userRepository.searchEmployees(keyword, vaiTro, null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tongSo", users.size());
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("hoTen", u.getHoTen());
            e.put("email", u.getEmail());
            e.put("sdt", u.getSdt());
            e.put("vaiTro", u.getVaiTro());
            e.put("trangThai", u.getTrangThai());
            if (u.getAssignedMotels() != null && !u.getAssignedMotels().isEmpty()) {
                List<String> motelNames = u.getAssignedMotels().stream()
                        .map(m -> m.getTenTro())
                        .toList();
                e.put("nhaTroPhuTrach", motelNames);
            }
            list.add(e);
        }
        data.put("danhSach", list);
        return data;
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.hasNonNull(field)) return null;
        String v = n.get(field).asText();
        return (v == null || v.isBlank()) ? null : v;
    }
}
