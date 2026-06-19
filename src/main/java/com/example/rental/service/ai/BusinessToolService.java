package com.example.rental.service.ai;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.RoomStatus;
import com.example.rental.domain.UserRole;
import com.example.rental.domain.WaterCalculationType;
import com.example.rental.dto.ContractRequest;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.MotelRequest;
import com.example.rental.dto.RoomRequest;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.service.ContractService;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.MotelService;
import com.example.rental.service.RoomService;
import com.example.rental.service.TenantService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level business operations. Every method is a "Business Tool" that:
 *   1. Resolves human-friendly identifiers (room code, tenant name, ...) to IDs
 *      via {@link ResolverService} -- the LLM never passes an ID.
 *   2. Enforces role-based permissions.
 *   3. Validates business rules.
 *   4. Calls the appropriate domain service in a single transaction.
 *   5. Returns a structured result the Planner can summarise in Vietnamese.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessToolService {

    private final ResolverService resolver;
    private final RoomService roomService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final ContractRepository contractRepository;
    private final InvoiceService invoiceService;
    private final MotelService motelService;

    public Map<String, Object> getRoomOverview(JsonNode args, User user) throws Exception {
        String roomCode = text(args, "maPhong");
        String motelName = text(args, "tenNhaTro");
        Room room = resolver.resolveRoom(roomCode, motelName);
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
                contractService.findByTrangThai(ContractStatus.DANG_HIEU_LUC).stream()
                        .filter(c -> c.getKhachThue() != null
                                && c.getKhachThue().getId().equals(tenant.getId()))
                        .toList());

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
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phong", room.getMaPhong());
        data.put("nhaTro", room.getNhaTro() != null ? room.getNhaTro().getTenTro() : "N/A");
        data.put("trangThai", room.getTrangThai());
        data.put("giaThue", room.getGiaThue());
        data.put("dienTich", room.getDienTich());
        data.put("soNguoi", room.getSoNguoi());
        return data;
    }

    public Map<String, Object> createContractForRoom(JsonNode args, User user) throws Exception {
        Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
        if (room.getTrangThai() == RoomStatus.DANG_THUE) {
            List<Contract> active = contractRepository.findByPhongTroIdAndTrangThai(
                    room.getId(), ContractStatus.DANG_HIEU_LUC);
            if (!active.isEmpty()) {
                throw new IllegalArgumentException(
                        "Phong " + room.getMaPhong() + " dang co hop dong hieu luc. Hay tra phong truoc.");
            }
        }
        if (room.getTrangThai() == RoomStatus.BAO_TRI) {
            throw new IllegalArgumentException("Phong " + room.getMaPhong() + " dang bao tri");
        }

        Tenant tenant;
        if (args.hasNonNull("maKhachThue")) {
            tenant = tenantService.findById(args.get("maKhachThue").asLong());
        } else {
            tenant = resolver.resolveTenant(
                    text(args, "hoTen"),
                    text(args, "sdt"),
                    text(args, "cccd"));
        }

        LocalDate start = parseDate(args, "ngayBatDau", LocalDate.now());
        Integer months = args.hasNonNull("soThang") ? args.get("soThang").asInt() : 12;
        LocalDate end = parseDate(args, "ngayKetThuc", start.plusMonths(months));
        BigDecimal giaThue = args.hasNonNull("giaThue")
                ? new BigDecimal(args.get("giaThue").asText())
                : room.getGiaThue();
        BigDecimal tienCoc = args.hasNonNull("tienCoc")
                ? new BigDecimal(args.get("tienCoc").asText())
                : BigDecimal.ZERO;

        ContractRequest req = new ContractRequest();
        req.setMaKhachThue(tenant.getId());
        req.setMaPhongTro(room.getId());
        req.setNgayBatDau(start);
        req.setNgayKetThuc(end);
        req.setGiaThue(giaThue);
        req.setTienCoc(tienCoc);
        req.setChuKyThanhToan(1);
        req.setNgayThanhToan(end.getDayOfMonth());

        Contract created = contractService.createContract(req, user);

        room.setTrangThai(RoomStatus.DANG_THUE);
        roomService.updateRoom(room.getId(), toRoomRequest(room));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHopDong", created.getMaHopDong());
        data.put("phong", room.getMaPhong());
        data.put("khach", tenant.getHoTen());
        data.put("ngayBatDau", start);
        data.put("ngayKetThuc", end);
        data.put("giaThue", giaThue);
        return data;
    }

    public Map<String, Object> renewContract(JsonNode args, User user) throws Exception {
        Contract contract = resolver.resolveContractByCode(text(args, "maHopDong"));
        if (contract.getTrangThai() != ContractStatus.DANG_HIEU_LUC) {
            throw new IllegalArgumentException(
                    "Hop dong " + contract.getMaHopDong() + " khong o trang thai hieu luc");
        }
        Integer months = args.hasNonNull("soThang") ? args.get("soThang").asInt() : 12;
        LocalDate base = contract.getNgayKetThuc() != null && contract.getNgayKetThuc().isAfter(LocalDate.now())
                ? contract.getNgayKetThuc()
                : LocalDate.now();
        LocalDate newEnd = base.plusMonths(months);

        ContractRequest req = new ContractRequest();
        req.setMaKhachThue(contract.getKhachThue().getId());
        req.setMaPhongTro(contract.getPhongTro().getId());
        req.setNgayBatDau(contract.getNgayBatDau());
        req.setNgayKetThuc(newEnd);
        req.setGiaThue(contract.getGiaThue());
        req.setTienCoc(contract.getTienCoc());
        req.setChuKyThanhToan(contract.getChuKyThanhToan());
        req.setNgayThanhToan(contract.getNgayThanhToan());

        Contract updated = contractService.updateContract(contract.getId(), req, user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHopDong", updated.getMaHopDong());
        data.put("ngayKetThucCu", contract.getNgayKetThuc());
        data.put("ngayKetThucMoi", newEnd);
        data.put("soThangGiaHan", months);
        return data;
    }

    public Map<String, Object> extendContract(JsonNode args, User user) throws Exception {
        return renewContract(args, user);
    }

    public Map<String, Object> terminateContract(JsonNode args, User user) throws Exception {
        Contract contract = resolver.resolveContractByCode(text(args, "maHopDong"));
        String lyDo = text(args, "lyDo");
        if (lyDo == null) lyDo = "Tra phong theo yeu cau AI";
        contractService.cancelContract(contract.getId(), lyDo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHopDong", contract.getMaHopDong());
        data.put("lyDo", lyDo);
        return data;
    }

    public Map<String, Object> closeContract(JsonNode args, User user) throws Exception {
        return terminateContract(args, user);
    }

    public Map<String, Object> checkoutTenant(JsonNode args, User user) throws Exception {
        Contract contract;
        if (args.hasNonNull("maHopDong")) {
            contract = resolver.resolveContractByCode(args.get("maHopDong").asText());
        } else {
            Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
            contract = resolver.resolveCurrentContract(room);
        }
        String lyDo = text(args, "lyDo");
        if (lyDo == null) lyDo = "Tra phong theo yeu cau";
        contractService.cancelContract(contract.getId(), lyDo);

        Room room = contract.getPhongTro();
        if (room != null) {
            room.setTrangThai(RoomStatus.TRONG);
            roomService.updateRoom(room.getId(), toRoomRequest(room));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHopDong", contract.getMaHopDong());
        data.put("phong", room != null ? room.getMaPhong() : "N/A");
        data.put("trangThaiPhongMoi", room != null ? room.getTrangThai() : null);
        return data;
    }

    public Map<String, Object> createInvoiceForRoom(JsonNode args, User user) throws Exception {
        Room room = resolver.resolveRoom(text(args, "maPhong"), text(args, "tenNhaTro"));
        Contract contract = resolver.resolveCurrentContract(room);

        InvoiceRequest req = new InvoiceRequest();
        req.setMaHopDong(contract.getId());

        if (args.hasNonNull("kyHoaDon")) {
            req.setKyHoaDon(DateTimeNormalizer.parseMonth(args.get("kyHoaDon").asText()));
        } else {
            Invoice latest = resolver.findLatestInvoice(contract);
            req.setKyHoaDon(latest != null && latest.getKyHoaDon() != null
                    ? latest.getKyHoaDon().plusMonths(1)
                    : LocalDate.now().withDayOfMonth(1));
        }

        Invoice latest = resolver.findLatestInvoice(contract);
        int dienCu = latest != null && latest.getChiSoDienMoi() != null ? latest.getChiSoDienMoi() : 0;
        int nuocCu = latest != null && latest.getChiSoNuocMoi() != null ? latest.getChiSoNuocMoi() : 0;
        req.setChiSoDienCu(dienCu);
        req.setChiSoNuocCu(nuocCu);

        // Resolve water calculation mode from contract (preferred) or args.
        WaterCalculationType kieuTinhNuoc = contract.getKieuTinhNuoc() != null
                ? contract.getKieuTinhNuoc()
                : WaterCalculationType.CHI_SO;
        if (args.hasNonNull("kieuTinhNuoc")) {
            kieuTinhNuoc = WaterCalculationType.valueOf(args.get("kieuTinhNuoc").asText());
        } else if (latest != null && latest.getKieuTinhNuoc() != null) {
            kieuTinhNuoc = latest.getKieuTinhNuoc();
        }
        req.setKieuTinhNuoc(kieuTinhNuoc);

        // ----- Validate required readings based on the contract configuration -----
        // Electricity is always billed by meter reading in this system.
        if (args.hasNonNull("chiSoDienMoi")) {
            req.setChiSoDienMoi(args.get("chiSoDienMoi").asInt());
        } else {
            throw new IllegalArgumentException("Can cung cap chiSoDienMoi");
        }
        // Water reading is only required when billed by meter.
        if (kieuTinhNuoc == WaterCalculationType.CHI_SO) {
            if (args.hasNonNull("chiSoNuocMoi")) {
                req.setChiSoNuocMoi(args.get("chiSoNuocMoi").asInt());
            } else {
                throw new IllegalArgumentException("Can cung cap chiSoNuocMoi");
            }
        } else {
            // Flat / per-person billing -> no meter reading needed.
            if (args.hasNonNull("chiSoNuocMoi")) {
                req.setChiSoNuocMoi(args.get("chiSoNuocMoi").asInt());
            }
        }

        if (args.hasNonNull("giaDien")) {
            req.setGiaDien(new BigDecimal(args.get("giaDien").asText()));
        } else if (latest != null && latest.getGiaDien() != null) {
            req.setGiaDien(latest.getGiaDien());
        }
        if (args.hasNonNull("giaNuoc")) {
            req.setGiaNuoc(new BigDecimal(args.get("giaNuoc").asText()));
        } else if (latest != null && latest.getGiaNuoc() != null) {
            req.setGiaNuoc(latest.getGiaNuoc());
        }
        if (args.hasNonNull("tienPhong")) {
            req.setTienPhong(new BigDecimal(args.get("tienPhong").asText()));
        } else {
            req.setTienPhong(contract.getGiaThue() != null ? contract.getGiaThue() : room.getGiaThue());
        }
        if (args.hasNonNull("hanThanhToan")) {
            req.setHanThanhToan(DateTimeNormalizer.parse(args.get("hanThanhToan").asText()));
        } else {
            req.setHanThanhToan(req.getKyHoaDon().plusDays(10));
        }
        if (args.hasNonNull("ghiChu")) {
            req.setGhiChu(args.get("ghiChu").asText());
        }

        Invoice created = invoiceService.createInvoice(req, user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHoaDon", created.getMaHoaDon());
        data.put("phong", room.getMaPhong());
        data.put("kyHoaDon", created.getKyHoaDon());
        data.put("chiSoDienCu", created.getChiSoDienCu());
        data.put("chiSoDienMoi", created.getChiSoDienMoi());
        data.put("chiSoNuocCu", created.getChiSoNuocCu());
        data.put("chiSoNuocMoi", created.getChiSoNuocMoi());
        data.put("giaDien", created.getGiaDien());
        data.put("giaNuoc", created.getGiaNuoc());
        data.put("tienPhong", created.getTienPhong());
        data.put("tongTien", created.getTongTien());
        data.put("hanThanhToan", created.getHanThanhToan());
        data.put("soDong", created.getDanhSachDichVu() != null ? created.getDanhSachDichVu().size() : 0);
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
        InvoiceRequest req = new InvoiceRequest();
        req.setMaHopDong(contract.getId());
        if (args.hasNonNull("chiSoDienMoi")) req.setChiSoDienMoi(args.get("chiSoDienMoi").asInt());
        if (args.hasNonNull("chiSoNuocMoi")) req.setChiSoNuocMoi(args.get("chiSoNuocMoi").asInt());
        if (args.hasNonNull("giaDien")) req.setGiaDien(new BigDecimal(args.get("giaDien").asText()));
        if (args.hasNonNull("giaNuoc")) req.setGiaNuoc(new BigDecimal(args.get("giaNuoc").asText()));
        if (args.hasNonNull("kieuTinhNuoc")) req.setKieuTinhNuoc(WaterCalculationType.valueOf(args.get("kieuTinhNuoc").asText()));
        if (args.hasNonNull("tienPhong")) req.setTienPhong(new BigDecimal(args.get("tienPhong").asText()));
        else req.setTienPhong(contract.getGiaThue() != null ? contract.getGiaThue() : contract.getPhongTro().getGiaThue());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tienPhong", req.getTienPhong());
        data.put("tienDien", invoiceService.computeElectricAmount(contract, req));
        data.put("tienNuoc", invoiceService.computeWaterAmount(contract, req));
        data.put("tienDichVu", invoiceService.computeServiceAmount(req));
        data.put("tong", invoiceService.computeTotal(contract, req));
        return data;
    }

    public Map<String, Object> collectPayment(JsonNode args, User user) throws Exception {
        Invoice invoice = resolver.resolveInvoice(text(args, "maHoaDon"));
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new IllegalArgumentException("Hoa don " + invoice.getMaHoaDon() + " da duoc thanh toan");
        }
        Invoice paid = invoiceService.markAsPaid(invoice.getId(), user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHoaDon", paid.getMaHoaDon());
        data.put("tongTien", paid.getTongTien());
        data.put("trangThaiMoi", paid.getTrangThai());
        return data;
    }

    public Map<String, Object> createMotel(JsonNode args, User user) throws Exception {
        if (user == null || user.getVaiTro() != UserRole.QUAN_LY) {
            throw new IllegalArgumentException("Chi quan ly moi co quyen tao nha tro");
        }
        MotelRequest req = new MotelRequest();
        if (args.hasNonNull("tenTro")) req.setTenTro(args.get("tenTro").asText());
        if (args.hasNonNull("diaChi")) req.setDiaChi(args.get("diaChi").asText());
        if (args.hasNonNull("soTang")) req.setSoTang(args.get("soTang").asInt());
        if (args.hasNonNull("tongPhong")) req.setTongPhong(args.get("tongPhong").asInt());
        if (args.hasNonNull("ghiChu")) req.setGhiChu(args.get("ghiChu").asText());
        if (req.getTenTro() == null || req.getTenTro().isBlank()) {
            throw new IllegalArgumentException("Thieu ten nha tro");
        }
        Motel created = motelService.createMotel(req, user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", created.getId());
        data.put("tenTro", created.getTenTro());
        data.put("diaChi", created.getDiaChi());
        return data;
    }

    public Map<String, Object> createRoom(JsonNode args, User user) throws Exception {
        RoomRequest req = new RoomRequest();
        if (args.hasNonNull("maPhong")) req.setMaPhong(args.get("maPhong").asText());
        if (args.hasNonNull("maNhaTro")) req.setMaNhaTro(args.get("maNhaTro").asLong());
        if (args.hasNonNull("tenNhaTro")) {
            Motel motel = resolver.resolveMotel(args.get("tenNhaTro").asText());
            req.setMaNhaTro(motel.getId());
        }
        if (args.hasNonNull("giaThue")) req.setGiaThue(new BigDecimal(args.get("giaThue").asText()));
        if (args.hasNonNull("dienTich")) req.setDienTich(new BigDecimal(args.get("dienTich").asText()));
        if (args.hasNonNull("soNguoi")) req.setSoNguoi(args.get("soNguoi").asInt());
        if (args.hasNonNull("tang")) req.setTang(args.get("tang").asInt());
        if (args.hasNonNull("ghiChu")) req.setGhiChu(args.get("ghiChu").asText());
        if (req.getMaNhaTro() == null) throw new IllegalArgumentException("Thieu nha tro");
        if (req.getMaPhong() == null || req.getMaPhong().isBlank()) throw new IllegalArgumentException("Thieu ma phong");
        if (req.getGiaThue() == null) throw new IllegalArgumentException("Thieu gia thue");
        Room created = roomService.createRoom(req);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", created.getId());
        data.put("maPhong", created.getMaPhong());
        data.put("nhaTro", created.getNhaTro() != null ? created.getNhaTro().getTenTro() : null);
        return data;
    }

    public Map<String, Object> createTenant(JsonNode args, User user) throws Exception {
        TenantRequest req = new TenantRequest();
        if (args.hasNonNull("hoTen")) req.setHoTen(args.get("hoTen").asText());
        if (args.hasNonNull("ngaySinh")) req.setNgaySinh(DateTimeNormalizer.parse(args.get("ngaySinh").asText()));
        if (args.hasNonNull("gioiTinh")) req.setGioiTinh(com.example.rental.domain.Gender.valueOf(args.get("gioiTinh").asText()));
        if (args.hasNonNull("cccd")) req.setCccd(args.get("cccd").asText());
        if (args.hasNonNull("sdt")) req.setSdt(args.get("sdt").asText());
        if (args.hasNonNull("email")) req.setEmail(args.get("email").asText());
        if (args.hasNonNull("diaChi")) req.setDiaChi(args.get("diaChi").asText());
        if (args.hasNonNull("ghiChu")) req.setGhiChu(args.get("ghiChu").asText());
        if (req.getHoTen() == null || req.getHoTen().isBlank()) {
            throw new IllegalArgumentException("Thieu ho ten khach thue");
        }
        Tenant created = tenantService.createTenant(req);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", created.getId());
        data.put("hoTen", created.getHoTen());
        return data;
    }

    public Map<String, Object> assignTenantToRoom(JsonNode args, User user) throws Exception {
        return createContractForRoom(args, user);
    }

    public Map<String, Object> moveTenant(JsonNode args, User user) throws Exception {
        if (!args.hasNonNull("maHopDong")) {
            throw new IllegalArgumentException("Can chi ro ma hop dong can chuyen phong");
        }
        Contract old = resolver.resolveContractByCode(args.get("maHopDong").asText());
        Room newRoom = resolver.resolveRoom(text(args, "maPhongMoi"), text(args, "tenNhaTroMoi"));
        if (newRoom.getTrangThai() == RoomStatus.DANG_THUE) {
            throw new IllegalArgumentException("Phong moi dang co nguoi thue");
        }
        contractService.cancelContract(old.getId(), "Chuyen sang phong " + newRoom.getMaPhong());

        ContractRequest req = new ContractRequest();
        req.setMaKhachThue(old.getKhachThue().getId());
        req.setMaPhongTro(newRoom.getId());
        req.setNgayBatDau(LocalDate.now());
        req.setNgayKetThuc(old.getNgayKetThuc() != null && old.getNgayKetThuc().isAfter(LocalDate.now())
                ? old.getNgayKetThuc() : LocalDate.now().plusMonths(12));
        req.setGiaThue(newRoom.getGiaThue());
        req.setTienCoc(old.getTienCoc());
        req.setChuKyThanhToan(old.getChuKyThanhToan());
        req.setNgayThanhToan(old.getNgayThanhToan());
        Contract created = contractService.createContract(req, user);

        Room oldRoom = old.getPhongTro();
        if (oldRoom != null) {
            oldRoom.setTrangThai(RoomStatus.TRONG);
            roomService.updateRoom(oldRoom.getId(), toRoomRequest(oldRoom));
        }
        newRoom.setTrangThai(RoomStatus.DANG_THUE);
        roomService.updateRoom(newRoom.getId(), toRoomRequest(newRoom));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("khach", old.getKhachThue().getHoTen());
        data.put("phongCu", oldRoom != null ? oldRoom.getMaPhong() : "N/A");
        data.put("phongMoi", newRoom.getMaPhong());
        data.put("maHopDongMoi", created.getMaHopDong());
        return data;
    }

    public Map<String, Object> generateInvoiceForMonth(JsonNode args, User user) throws Exception {
        return createInvoiceForRoom(args, user);
    }

    // ====================================================================
    //  Read-only: list occupants / contracts / find room of a tenant
    // ====================================================================

    public Map<String, Object> getActiveTenants(JsonNode args, User user) throws Exception {
        String motelName = text(args, "tenNhaTro");
        Long motelIdFilter = null;
        if (motelName != null && !motelName.isBlank()) {
            motelIdFilter = resolver.resolveMotel(motelName).getId();
        }

        List<Contract> all = contractService.findByTrangThai(ContractStatus.DANG_HIEU_LUC);

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

    // ====================================================================
    //  Mutations
    // ====================================================================

    public Map<String, Object> updateInvoice(JsonNode args, User user) throws Exception {
        Invoice invoice = resolver.resolveInvoice(text(args, "maHoaDon"));
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new IllegalArgumentException("Hoa don da thanh toan, khong the cap nhat");
        }
        InvoiceRequest req = new InvoiceRequest();
        req.setMaHopDong(invoice.getHopDong() != null ? invoice.getHopDong().getId() : null);
        req.setKyHoaDon(args.hasNonNull("kyHoaDon")
                ? DateTimeNormalizer.parseMonth(args.get("kyHoaDon").asText())
                : invoice.getKyHoaDon());
        req.setChiSoDienCu(args.hasNonNull("chiSoDienCu") ? args.get("chiSoDienCu").asInt() : invoice.getChiSoDienCu());
        req.setChiSoDienMoi(args.hasNonNull("chiSoDienMoi") ? args.get("chiSoDienMoi").asInt() : invoice.getChiSoDienMoi());
        req.setGiaDien(args.hasNonNull("giaDien")
                ? new BigDecimal(args.get("giaDien").asText())
                : invoice.getGiaDien());
        req.setChiSoNuocCu(args.hasNonNull("chiSoNuocCu") ? args.get("chiSoNuocCu").asInt() : invoice.getChiSoNuocCu());
        req.setChiSoNuocMoi(args.hasNonNull("chiSoNuocMoi") ? args.get("chiSoNuocMoi").asInt() : invoice.getChiSoNuocMoi());
        req.setGiaNuoc(args.hasNonNull("giaNuoc")
                ? new BigDecimal(args.get("giaNuoc").asText())
                : invoice.getGiaNuoc());
        if (args.hasNonNull("kieuTinhNuoc")) {
            req.setKieuTinhNuoc(WaterCalculationType.valueOf(args.get("kieuTinhNuoc").asText()));
        } else {
            req.setKieuTinhNuoc(invoice.getKieuTinhNuoc());
        }
        req.setTienPhong(args.hasNonNull("tienPhong")
                ? new BigDecimal(args.get("tienPhong").asText())
                : invoice.getTienPhong());
        req.setHanThanhToan(args.hasNonNull("hanThanhToan")
                ? DateTimeNormalizer.parse(args.get("hanThanhToan").asText())
                : invoice.getHanThanhToan());
        if (args.hasNonNull("ghiChu")) req.setGhiChu(args.get("ghiChu").asText());

        Invoice updated = invoiceService.updateInvoice(invoice.getId(), req, user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHoaDon", updated.getMaHoaDon());
        data.put("tongTien", updated.getTongTien());
        data.put("trangThai", updated.getTrangThai());
        return data;
    }

    public Map<String, Object> deleteInvoice(JsonNode args, User user) throws Exception {
        Invoice invoice = resolver.resolveInvoice(text(args, "maHoaDon"));
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new IllegalArgumentException("Hoa don da thanh toan, khong the xoa");
        }
        invoiceService.deleteInvoice(invoice.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maHoaDon", invoice.getMaHoaDon());
        data.put("xoa", true);
        return data;
    }

    public Map<String, Object> createInvoiceByRoom(JsonNode args, User user) throws Exception {
        // Stronger alias: identical to createInvoiceForRoom but the LLM is told
        // explicitly that the room is the only required handle. Meter readings and
        // kỳ hóa đơn are still asked from the user.
        return createInvoiceForRoom(args, user);
    }

    public Map<String, Object> createTransactionForInvoice(JsonNode args, User user) throws Exception {
        Invoice invoice = resolver.resolveInvoice(text(args, "maHoaDon"));
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new IllegalArgumentException("Hoa don da duoc thanh toan");
        }
        return collectPayment(args, user);
    }

    private RoomRequest toRoomRequest(Room r) {
        RoomRequest req = new RoomRequest();
        if (r.getNhaTro() != null) req.setMaNhaTro(r.getNhaTro().getId());
        req.setMaPhong(r.getMaPhong());
        req.setGiaThue(r.getGiaThue());
        req.setDienTich(r.getDienTich());
        req.setSoNguoi(r.getSoNguoi());
        req.setTang(r.getTang());
        req.setGhiChu(r.getGhiChu());
        return req;
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.hasNonNull(field)) return null;
        String v = n.get(field).asText();
        return (v == null || v.isBlank()) ? null : v;
    }

    private static LocalDate parseDate(JsonNode n, String field, LocalDate fallback) {
        if (n != null && n.hasNonNull(field)) {
            return DateTimeNormalizer.parse(n.get(field).asText());
        }
        return fallback;
    }
}
