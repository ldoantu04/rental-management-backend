package com.example.rental.service.ai;

import com.example.rental.domain.MotelStatus;
import com.example.rental.domain.UserRole;
import com.example.rental.dto.ContractRequest;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.MotelRequest;
import com.example.rental.dto.RoomRequest;
import com.example.rental.dto.TenantRequest;
import com.example.rental.model.Motel;
import com.example.rental.model.User;
import com.example.rental.service.ContractService;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.MotelService;
import com.example.rental.service.RoomService;
import com.example.rental.service.TenantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Executes business mutations on behalf of the AI after the user has confirmed.
 *
 * <p>Each method corresponds to a {@code hanhDong} code returned by the AI
 * (e.g. {@code CREATE_ROOM}). Permission is checked against the authenticated
 * user's role.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatActionService {

    private final RoomService roomService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final MotelService motelService;
    private final com.example.rental.service.ai.ResolverService resolver;
    private final ObjectMapper objectMapper;

    public Object execute(String hanhDong, String payloadJson, User user) throws Exception {
        if (hanhDong == null || hanhDong.isBlank()) {
            throw new IllegalArgumentException("Hanh dong khong hop le");
        }
        JsonNode payload = payloadJson == null || payloadJson.isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(payloadJson);

        return switch (hanhDong.toUpperCase()) {
            case "CREATE_MOTEL" -> {
                requireManager(user);
                yield createMotel(payload, user);
            }
            case "CREATE_ROOM" -> {
                yield createRoom(payload, user);
            }
            case "CREATE_TENANT" -> {
                yield createTenant(payload, user);
            }
            case "CREATE_CONTRACT" -> {
                yield createContract(payload, user);
            }
            case "CREATE_INVOICE" -> {
                yield createInvoice(payload, user);
            }
            case "DELETE_ROOM", "DELETE_TENANT", "DELETE_CONTRACT", "DELETE_INVOICE", "DELETE_MOTEL" -> {
                throw new IllegalArgumentException("Vui long su dung giao dien de xoa " + hanhDong.replace("DELETE_", "").toLowerCase());
            }
            default -> throw new IllegalArgumentException("Hanh dong khong duoc ho tro: " + hanhDong);
        };
    }

    private void requireManager(User user) {
        if (user == null || user.getVaiTro() != UserRole.QUAN_LY) {
            throw new IllegalArgumentException("Chi quan ly moi co quyen thuc hien thao tac nay");
        }
    }

    private Motel createMotel(JsonNode p, User user) throws Exception {
        MotelRequest req = new MotelRequest();
        if (p.hasNonNull("tenTro")) req.setTenTro(p.get("tenTro").asText());
        if (p.hasNonNull("diaChi")) req.setDiaChi(p.get("diaChi").asText());
        if (p.hasNonNull("soTang")) req.setSoTang(p.get("soTang").asInt());
        if (p.hasNonNull("tongPhong")) req.setTongPhong(p.get("tongPhong").asInt());
        if (p.hasNonNull("trangThai")) req.setTrangThai(MotelStatus.valueOf(p.get("trangThai").asText()));
        else req.setTrangThai(MotelStatus.HOAT_DONG);
        if (p.hasNonNull("ghiChu")) req.setGhiChu(p.get("ghiChu").asText());
        if (req.getTenTro() == null || req.getTenTro().isBlank()) {
            throw new IllegalArgumentException("Thieu ten nha tro");
        }
        return motelService.createMotel(req, user);
    }

    private com.example.rental.model.Room createRoom(JsonNode p, User user) throws Exception {
        RoomRequest req = new RoomRequest();
        if (p.hasNonNull("maNhaTro")) req.setMaNhaTro(p.get("maNhaTro").asLong());
        if (p.hasNonNull("tenNhaTro")) {
            com.example.rental.model.Motel motel = resolver.resolveMotel(p.get("tenNhaTro").asText());
            req.setMaNhaTro(motel.getId());
        }
        if (p.hasNonNull("maPhong")) req.setMaPhong(p.get("maPhong").asText());
        if (p.hasNonNull("giaThue")) req.setGiaThue(new BigDecimal(p.get("giaThue").asText()));
        if (p.hasNonNull("dienTich")) req.setDienTich(new BigDecimal(p.get("dienTich").asText()));
        if (p.hasNonNull("soNguoi")) req.setSoNguoi(p.get("soNguoi").asInt());
        if (p.hasNonNull("tang")) req.setTang(p.get("tang").asInt());
        if (p.hasNonNull("ghiChu")) req.setGhiChu(p.get("ghiChu").asText());
        if (req.getMaNhaTro() == null) throw new IllegalArgumentException("Thieu ma nha tro");
        if (req.getMaPhong() == null || req.getMaPhong().isBlank()) throw new IllegalArgumentException("Thieu ma phong");
        if (req.getGiaThue() == null) throw new IllegalArgumentException("Thieu gia thue");
        return roomService.createRoom(req);
    }

    private com.example.rental.model.Tenant createTenant(JsonNode p, User user) throws Exception {
        TenantRequest req = new TenantRequest();
        if (p.hasNonNull("hoTen")) req.setHoTen(p.get("hoTen").asText());
        if (p.hasNonNull("ngaySinh")) req.setNgaySinh(com.example.rental.service.ai.DateTimeNormalizer.parse(p.get("ngaySinh").asText()));
        if (p.hasNonNull("gioiTinh")) req.setGioiTinh(com.example.rental.domain.Gender.valueOf(p.get("gioiTinh").asText()));
        if (p.hasNonNull("cccd")) req.setCccd(p.get("cccd").asText());
        if (p.hasNonNull("sdt")) req.setSdt(p.get("sdt").asText());
        if (p.hasNonNull("email")) req.setEmail(p.get("email").asText());
        if (p.hasNonNull("diaChi")) req.setDiaChi(p.get("diaChi").asText());
        if (p.hasNonNull("ghiChu")) req.setGhiChu(p.get("ghiChu").asText());
        if (req.getHoTen() == null || req.getHoTen().isBlank()) {
            throw new IllegalArgumentException("Thieu ho ten khach thue");
        }
        return tenantService.createTenant(req);
    }

    private com.example.rental.model.Contract createContract(JsonNode p, User user) throws Exception {
        ContractRequest req = new ContractRequest();
        if (p.hasNonNull("maKhachThue")) req.setMaKhachThue(p.get("maKhachThue").asLong());
        if (p.hasNonNull("maPhongTro")) req.setMaPhongTro(p.get("maPhongTro").asLong());
        if (p.hasNonNull("ngayBatDau")) req.setNgayBatDau(com.example.rental.service.ai.DateTimeNormalizer.parse(p.get("ngayBatDau").asText()));
        if (p.hasNonNull("ngayKetThuc")) req.setNgayKetThuc(com.example.rental.service.ai.DateTimeNormalizer.parse(p.get("ngayKetThuc").asText()));
        if (p.hasNonNull("tienCoc")) req.setTienCoc(new BigDecimal(p.get("tienCoc").asText()));
        if (p.hasNonNull("giaThue")) req.setGiaThue(new BigDecimal(p.get("giaThue").asText()));
        if (p.hasNonNull("chuKyThanhToan")) req.setChuKyThanhToan(p.get("chuKyThanhToan").asInt());
        if (p.hasNonNull("ngayThanhToan")) req.setNgayThanhToan(p.get("ngayThanhToan").asInt());
        if (p.hasNonNull("dieuKhoan")) req.setDieuKhoan(p.get("dieuKhoan").asText());
        if (req.getMaKhachThue() == null) throw new IllegalArgumentException("Thieu ma khach thue");
        if (req.getMaPhongTro() == null) throw new IllegalArgumentException("Thieu ma phong tro");
        if (req.getNgayBatDau() == null) throw new IllegalArgumentException("Thieu ngay bat dau");
        if (req.getNgayKetThuc() == null) throw new IllegalArgumentException("Thieu ngay ket thuc");
        if (req.getGiaThue() == null) throw new IllegalArgumentException("Thieu gia thue");
        return contractService.createContract(req, user);
    }

    private com.example.rental.model.Invoice createInvoice(JsonNode p, User user) throws Exception {
        InvoiceRequest req = new InvoiceRequest();
        if (p.hasNonNull("maHopDong")) {
            req.setMaHopDong(p.get("maHopDong").asLong());
        }
        if (req.getMaHopDong() == null) {
            // Fallback: resolve from room when the LLM forgot to fill the contract id.
            String roomCode = p.hasNonNull("maPhong") ? p.get("maPhong").asText() : null;
            String motelName = p.hasNonNull("tenNhaTro") ? p.get("tenNhaTro").asText() : null;
            if (roomCode != null && !roomCode.isBlank()) {
                com.example.rental.model.Room room = resolver.resolveRoom(roomCode, motelName);
                com.example.rental.model.Contract contract = resolver.resolveCurrentContract(room);
                req.setMaHopDong(contract.getId());
            }
        }
        if (req.getMaHopDong() == null) throw new IllegalArgumentException("Thieu ma hop dong");
        if (p.hasNonNull("kyHoaDon")) req.setKyHoaDon(com.example.rental.service.ai.DateTimeNormalizer.parseMonth(p.get("kyHoaDon").asText()));
        if (p.hasNonNull("chiSoDienCu")) req.setChiSoDienCu(p.get("chiSoDienCu").asInt());
        if (p.hasNonNull("chiSoDienMoi")) req.setChiSoDienMoi(p.get("chiSoDienMoi").asInt());
        if (p.hasNonNull("giaDien")) req.setGiaDien(new BigDecimal(p.get("giaDien").asText()));
        if (p.hasNonNull("chiSoNuocCu")) req.setChiSoNuocCu(p.get("chiSoNuocCu").asInt());
        if (p.hasNonNull("chiSoNuocMoi")) req.setChiSoNuocMoi(p.get("chiSoNuocMoi").asInt());
        if (p.hasNonNull("giaNuoc")) req.setGiaNuoc(new BigDecimal(p.get("giaNuoc").asText()));
        if (p.hasNonNull("tienPhong")) req.setTienPhong(new BigDecimal(p.get("tienPhong").asText()));
        if (p.hasNonNull("hanThanhToan")) req.setHanThanhToan(com.example.rental.service.ai.DateTimeNormalizer.parse(p.get("hanThanhToan").asText()));
        if (p.hasNonNull("ghiChu")) req.setGhiChu(p.get("ghiChu").asText());
        return invoiceService.createInvoice(req, user);
    }
}
