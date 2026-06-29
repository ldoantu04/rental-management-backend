package com.example.rental.service.ai;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.domain.TenantStatus;
import com.example.rental.domain.UserRole;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only Spring AI tool callbacks used by the assistant to fetch real data
 * from the database instead of hallucinating answers.
 */
@Component
@RequiredArgsConstructor
public class ChatQueryTools {

    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final MotelRepository motelRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Tool(description = "Lay danh sach cac phong dang trong (co the loc theo nha tro). Tra ve danh sach phong trong de cho thue.")
    public String listAvailableRooms(
            @ToolParam(description = "Id nha tro de loc phong trong. Co the bo qua de lay tat ca.", required = false) Long motelId) {
        List<Room> rooms = roomRepository.findByTrangThai(RoomStatus.TRONG);
        if (motelId != null) {
            rooms = rooms.stream()
                    .filter(r -> r.getNhaTro() != null && motelId.equals(r.getNhaTro().getId()))
                    .collect(Collectors.toList());
        }
        if (rooms.isEmpty()) {
            return "Hien tai khong co phong trong nao.";
        }
        StringBuilder sb = new StringBuilder("Danh sach phong trong (")
                .append(rooms.size()).append(" phong):\n");
        int idx = 1;
        for (Room r : rooms) {
            sb.append(idx++).append(". Phong ").append(safe(r.getMaPhong()))
                    .append(" - Nha tro: ").append(safe(r.getNhaTro() != null ? r.getNhaTro().getTenTro() : "N/A"))
                    .append(" - Gia: ").append(formatMoney(r.getGiaThue()))
                    .append(" VND - Dien tich: ").append(r.getDienTich())
                    .append("m2\n");
        }
        return sb.toString();
    }

    @Tool(description = "Lay danh sach cac phong dang cho thue.")
    public String listRentedRooms() {
        List<Room> rooms = roomRepository.findByTrangThai(RoomStatus.DANG_THUE);
        if (rooms.isEmpty()) {
            return "Hien tai khong co phong nao dang cho thue.";
        }
        StringBuilder sb = new StringBuilder("Danh sach phong dang cho thue (")
                .append(rooms.size()).append(" phong):\n");
        int idx = 1;
        for (Room r : rooms) {
            sb.append(idx++).append(". Phong ").append(safe(r.getMaPhong()))
                    .append(" - Nha tro: ").append(safe(r.getNhaTro() != null ? r.getNhaTro().getTenTro() : "N/A"))
                    .append(" - Gia: ").append(formatMoney(r.getGiaThue()))
                    .append(" VND\n");
        }
        return sb.toString();
    }

    @Tool(description = "Thong ke so phong theo trang thai (trong, dang thue, bao tri).")
    public String countRoomsByStatus() {
        List<Room> all = roomRepository.findAll();
        long trong = all.stream().filter(r -> r.getTrangThai() == RoomStatus.TRONG).count();
        long dangThue = all.stream().filter(r -> r.getTrangThai() == RoomStatus.DANG_THUE).count();
        long baoTri = all.stream().filter(r -> r.getTrangThai() == RoomStatus.BAO_TRI).count();
        return "Tong so phong: " + all.size()
                + " | Phong trong: " + trong
                + " | Phong dang cho thue: " + dangThue
                + " | Phong bao tri: " + baoTri;
    }

    @Tool(description = "Lay danh sach khach thue. Co the loc theo trang thai (DANG_THUE, CHUA_NHAN_PHONG, DA_CHUYEN_DI).")
    public String listTenants(
            @ToolParam(description = "Loc theo trang thai khach thue (DANG_THUE, CHUA_NHAN_PHONG, DA_CHUYEN_DI). Co the bo qua.", required = false) String status) {
        List<Tenant> tenants = tenantRepository.findAll();
        if (status != null && !status.isBlank()) {
            TenantStatus tenantStatus = parseTenantStatus(status);
            if (tenantStatus != null) {
                tenants = tenants.stream().filter(t -> t.getTrangThai() == tenantStatus).collect(Collectors.toList());
            }
        }
        if (tenants.isEmpty()) {
            return "Khong tim thay khach thue phu hop.";
        }
        StringBuilder sb = new StringBuilder("Danh sach khach thue (")
                .append(tenants.size()).append(" nguoi):\n");
        int idx = 1;
        for (Tenant t : tenants) {
            sb.append(idx++).append(". ").append(safe(t.getHoTen()))
                    .append(" - SDT: ").append(safe(t.getSdt()))
                    .append(" - Trang thai: ").append(t.getTrangThai() != null ? t.getTrangThai().name() : "N/A")
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Lay cac hop dong sap het han trong vong N ngay toi (mac dinh 30).")
    public String listExpiringContracts(
            @ToolParam(description = "So ngay toi da den han (mac dinh 30).", required = false) Integer days) {
        int window = (days != null && days > 0) ? days : 30;
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(window);
        List<Contract> contracts = contractRepository.findAll().stream()
                .filter(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC)
                .filter(c -> c.getNgayKetThuc() != null)
                .filter(c -> !c.getNgayKetThuc().isBefore(today) && !c.getNgayKetThuc().isAfter(limit))
                .sorted(Comparator.comparing(Contract::getNgayKetThuc))
                .collect(Collectors.toList());
        if (contracts.isEmpty()) {
            return "Khong co hop dong nao sap het han trong " + window + " ngay toi.";
        }
        StringBuilder sb = new StringBuilder("Hop dong sap het han trong " + window + " ngay (")
                .append(contracts.size()).append(" hop dong):\n");
        int idx = 1;
        for (Contract c : contracts) {
            sb.append(idx++).append(". ").append(safe(c.getKhachThue() != null ? c.getKhachThue().getHoTen() : "N/A"))
                    .append(" - Phong: ").append(safe(c.getPhongTro() != null ? c.getPhongTro().getMaPhong() : "N/A"))
                    .append(" - Ma HD: ").append(safe(c.getMaHopDong()))
                    .append(" - Het han: ").append(c.getNgayKetThuc().format(DATE_FMT))
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Lay danh sach cac hoa don chua thanh toan hoac qua han.")
    public String listUnpaidInvoices() {
        List<InvoiceStatus> statuses = new ArrayList<>();
        statuses.add(InvoiceStatus.CHUA_THANH_TOAN);
        statuses.add(InvoiceStatus.QUA_HAN);
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getTrangThai() != null && statuses.contains(i.getTrangThai()))
                .sorted(Comparator.comparing(Invoice::getHanThanhToan, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        if (invoices.isEmpty()) {
            return "Khong co hoa don nao chua thanh toan.";
        }
        StringBuilder sb = new StringBuilder("Danh sach hoa don chua thanh toan (")
                .append(invoices.size()).append(" hoa don):\n");
        int idx = 1;
        for (Invoice inv : invoices) {
            sb.append(idx++).append(". Ma: ").append(safe(inv.getMaHoaDon()))
                    .append(" - Khach: ").append(safe(inv.getHopDong() != null && inv.getHopDong().getKhachThue() != null
                            ? inv.getHopDong().getKhachThue().getHoTen() : "N/A"))
                    .append(" - Phong: ").append(safe(inv.getHopDong() != null && inv.getHopDong().getPhongTro() != null
                            ? inv.getHopDong().getPhongTro().getMaPhong() : "N/A"))
                    .append(" - Tong: ").append(formatMoney(inv.getTongTien())).append(" VND")
                    .append(" - Trang thai: ").append(inv.getTrangThai())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Tinh doanh thu theo thang hoac nam hien tai (mac dinh thang).")
    public String getRevenue(
            @ToolParam(description = "Loai thoi gian: 'month' hoac 'year' (mac dinh month).", required = false) String period) {
        String actualPeriod = period != null ? period.toLowerCase() : "month";
        List<Invoice> all = invoiceRepository.findAll();
        LocalDate today = LocalDate.now();
        List<Invoice> paid = all.stream()
                .filter(i -> i.getTrangThai() == InvoiceStatus.DA_THANH_TOAN)
                .filter(i -> i.getNgayTao() != null)
                .collect(Collectors.toList());

        BigDecimal total;
        String label;
        if (actualPeriod.contains("nam") || actualPeriod.contains("year")) {
            total = paid.stream()
                    .filter(i -> i.getNgayTao().getYear() == today.getYear())
                    .map(i -> i.getTongTien() != null ? i.getTongTien() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            label = "Doanh thu nam " + today.getYear();
        } else {
            total = paid.stream()
                    .filter(i -> i.getNgayTao().getMonth() == today.getMonth()
                            && i.getNgayTao().getYear() == today.getYear())
                    .map(i -> i.getTongTien() != null ? i.getTongTien() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            label = "Doanh thu thang " + today.getMonthValue() + "/" + today.getYear();
        }
        return label + ": " + formatMoney(total) + " VND (tu " + paid.size() + " hoa don da thanh toan)";
    }

    @Tool(description = "Lay cac phong chua co hop dong hieu luc.")
    public String listRoomsWithoutContract() {
        List<Room> all = roomRepository.findAll();
        List<Room> noContract = new ArrayList<>();
        for (Room r : all) {
            List<Contract> active = contractRepository.findByPhongTroIdAndTrangThai(r.getId(), ContractStatus.DANG_HIEU_LUC);
            if (active == null || active.isEmpty()) {
                noContract.add(r);
            }
        }
        if (noContract.isEmpty()) {
            return "Tat ca cac phong deu co hop dong hieu luc.";
        }
        StringBuilder sb = new StringBuilder("Cac phong chua co hop dong (")
                .append(noContract.size()).append(" phong):\n");
        int idx = 1;
        for (Room r : noContract) {
            sb.append(idx++).append(". Phong ").append(safe(r.getMaPhong()))
                    .append(" - Nha tro: ").append(safe(r.getNhaTro() != null ? r.getNhaTro().getTenTro() : "N/A"))
                    .append(" - Trang thai: ").append(r.getTrangThai())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Lay khach thue dang no tien (co hoa don chua thanh toan hoac qua han).")
    public String listTenantsWithDebt() {
        List<InvoiceStatus> debtStatuses = new ArrayList<>();
        debtStatuses.add(InvoiceStatus.CHUA_THANH_TOAN);
        debtStatuses.add(InvoiceStatus.QUA_HAN);

        List<Invoice> debts = invoiceRepository.findAll().stream()
                .filter(i -> i.getTrangThai() != null && debtStatuses.contains(i.getTrangThai()))
                .collect(Collectors.toList());

        if (debts.isEmpty()) {
            return "Khong co khach thue nao dang no tien.";
        }

        StringBuilder sb = new StringBuilder("Khach thue dang no tien:\n");
        int idx = 1;
        for (Invoice inv : debts) {
            if (inv.getHopDong() == null || inv.getHopDong().getKhachThue() == null) continue;
            sb.append(idx++).append(". ").append(safe(inv.getHopDong().getKhachThue().getHoTen()))
                    .append(" - Phong: ").append(safe(inv.getHopDong().getPhongTro() != null
                            ? inv.getHopDong().getPhongTro().getMaPhong() : "N/A"))
                    .append(" - Ma HD: ").append(safe(inv.getMaHoaDon()))
                    .append(" - So tien: ").append(formatMoney(inv.getTongTien())).append(" VND")
                    .append(" - Trang thai: ").append(inv.getTrangThai())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Lay danh sach nha tro. Chi danh cho quan ly.")
    public String listMotels() {
        List<com.example.rental.model.Motel> motels = motelRepository.findAll();
        if (motels.isEmpty()) {
            return "He thong chua co nha tro nao.";
        }
        StringBuilder sb = new StringBuilder("Danh sach nha tro (").append(motels.size()).append("):\n");
        int idx = 1;
        for (com.example.rental.model.Motel m : motels) {
            sb.append(idx++).append(". ").append(safe(m.getTenTro()))
                    .append(" - Dia chi: ").append(safe(m.getDiaChi()))
                    .append(" - Tong phong: ").append(m.getTongPhong())
                    .append(" - Trang thai: ").append(m.getTrangThai())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Thong ke tong quan toan he thong. Chi danh cho quan ly.")
    public String getSystemStats() {
        LocalDate today = LocalDate.now();
        long tongPhong = roomRepository.count();
        long phongTrong = roomRepository.findByTrangThai(RoomStatus.TRONG).size();
        long phongDangThue = roomRepository.findByTrangThai(RoomStatus.DANG_THUE).size();

        long hopDongSapHet = contractRepository.findAll().stream()
                .filter(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC)
                .filter(c -> c.getNgayKetThuc() != null)
                .filter(c -> {
                    long days = ChronoUnit.DAYS.between(today, c.getNgayKetThuc());
                    return days >= 0 && days <= 30;
                }).count();

        long hoaDonChuaTT = invoiceRepository.findAll().stream()
                .filter(i -> i.getTrangThai() == InvoiceStatus.CHUA_THANH_TOAN
                        || i.getTrangThai() == InvoiceStatus.QUA_HAN)
                .count();

        BigDecimal doanhThuThang = invoiceRepository.findAll().stream()
                .filter(i -> i.getTrangThai() == InvoiceStatus.DA_THANH_TOAN)
                .filter(i -> i.getNgayTao() != null
                        && i.getNgayTao().getMonth() == today.getMonth()
                        && i.getNgayTao().getYear() == today.getYear())
                .map(i -> i.getTongTien() != null ? i.getTongTien() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "Tong quan he thong:\n"
                + "- Tong so phong: " + tongPhong
                + " (trong: " + phongTrong + ", dang cho thue: " + phongDangThue + ")\n"
                + "- Hop dong sap het han: " + hopDongSapHet + "\n"
                + "- Hoa don chua thanh toan: " + hoaDonChuaTT + "\n"
                + "- Doanh thu thang " + today.getMonthValue() + "/" + today.getYear()
                + ": " + formatMoney(doanhThuThang) + " VND";
    }

    private TenantStatus parseTenantStatus(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        if (v.contains("DANG") || v.contains("THUE")) return TenantStatus.DANG_THUE;
        if (v.contains("CHUA") || v.contains("NHAN")) return TenantStatus.CHUA_NHAN_PHONG;
        if (v.contains("CHUYEN") || v.contains("DA_CHUYEN")) return TenantStatus.DA_CHUYEN_DI;
        try {
            return TenantStatus.valueOf(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Tool(description = "Tra cuu danh sach nhan vien. Chi danh cho quan ly. Co the loc theo tu khoa hoac vai tro.")
    public String getEmployeeOverview(
            @ToolParam(description = "Tu khoa tra cuu (ho ten, email). Co the bo qua.", required = false) String keyword,
            @ToolParam(description = "Vai tro can loc: QUAN_LY hoac NHAN_VIEN. Bo qua se tra cuu nhan vien.", required = false) String vaiTro) {
        UserRole filterRole = UserRole.NHAN_VIEN;
        if (vaiTro != null && !vaiTro.isBlank()) {
            try {
                filterRole = UserRole.valueOf(vaiTro.toUpperCase());
            } catch (Exception ignored) {}
        }
        List<User> employees = userRepository.searchEmployees(keyword, filterRole, null);
        if (employees.isEmpty()) {
            return "Khong tim thay nhan vien phu hop.";
        }
        StringBuilder sb = new StringBuilder("Danh sach nhan vien (").append(employees.size()).append(" nguoi):\n");
        int idx = 1;
        for (User emp : employees) {
            sb.append(idx++).append(". ").append(safe(emp.getHoTen()))
                    .append(" - Email: ").append(safe(emp.getEmail()))
                    .append(" - SDT: ").append(safe(emp.getSdt()))
                    .append(" - Vai tro: ").append(emp.getVaiTro())
                    .append(" - Trang thai: ").append(emp.getTrangThai());
            if (emp.getAssignedMotels() != null && !emp.getAssignedMotels().isEmpty()) {
                sb.append(" - Nha tro: ");
                sb.append(emp.getAssignedMotels().stream()
                        .map(m -> m.getTenTro())
                        .collect(Collectors.joining(", ")));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Tra cuu danh sach tat ca tai khoan (nhan vien va quan ly). Chi danh cho quan ly.")
    public String getUserOverview(
            @ToolParam(description = "Tu khoa tra cuu (ho ten, email). Co the bo qua.", required = false) String keyword,
            @ToolParam(description = "Vai tro can loc: QUAN_LY hoac NHAN_VIEN. Bo qua se tra cuu tat ca.", required = false) String vaiTro) {
        UserRole filterRole = null;
        if (vaiTro != null && !vaiTro.isBlank()) {
            try {
                filterRole = UserRole.valueOf(vaiTro.toUpperCase());
            } catch (Exception ignored) {}
        }
        List<User> users = userRepository.searchEmployees(keyword, filterRole, null);
        if (users.isEmpty()) {
            return "Khong tim thay tai khoan phu hop.";
        }
        StringBuilder sb = new StringBuilder("Danh sach tai khoan (").append(users.size()).append(" tai khoan):\n");
        int idx = 1;
        for (User u : users) {
            sb.append(idx++).append(". ").append(safe(u.getHoTen()))
                    .append(" - Email: ").append(safe(u.getEmail()))
                    .append(" - SDT: ").append(safe(u.getSdt()))
                    .append(" - Vai tro: ").append(u.getVaiTro())
                    .append(" - Trang thai: ").append(u.getTrangThai());
            if (u.getAssignedMotels() != null && !u.getAssignedMotels().isEmpty()) {
                sb.append(" - Nha tro: ");
                sb.append(u.getAssignedMotels().stream()
                        .map(m -> m.getTenTro())
                        .collect(Collectors.joining(", ")));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "N/A" : s;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount.doubleValue());
    }
}
