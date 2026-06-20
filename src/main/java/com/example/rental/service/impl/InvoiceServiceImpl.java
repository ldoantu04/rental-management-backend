package com.example.rental.service.impl;

import com.example.rental.config.VNPayConfig;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.domain.WaterCalculationType;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.InvoiceServiceItemRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.model.Transaction;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.InvoiceServiceItemRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.EmailTemplateService;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.NotificationService;
import com.example.rental.service.UserService;
import com.example.rental.service.utils.InvoicePricingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final InvoiceServiceItemRepository invoiceServiceItemRepository;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final TransactionRepository transactionRepository;
    private final VNPayConfig vnPayConfig;
    private final EmailTemplateService emailTemplateService;
    private final NotificationService notificationService;
    private final UserService userService;

    private static final String TEMPLATE_PAYMENT_REMINDER = "NHAC_THANH_TOAN";

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    @Transactional
    public Invoice createInvoice(InvoiceRequest req) throws Exception {
        return createInvoice(req, null);
    }

    @Override
    @Transactional
    public Invoice createInvoice(InvoiceRequest req, User nguoiTao) throws Exception {
        Contract contract = contractRepository.findById(req.getMaHopDong())
                .orElseThrow(() -> new Exception("Khong tim thay hop dong voi id " + req.getMaHopDong()));

        long count = invoiceRepository.count();
        String maHoaDon = "HD" + String.format("%03d", count + 1);

        Invoice previous = findLatestByHopDongId(contract.getId());

        Invoice invoice = new Invoice();
        invoice.setMaHoaDon(maHoaDon);
        invoice.setHopDong(contract);
        invoice.setKyHoaDon(req.getKyHoaDon());
        invoice.setChiSoDienCu(req.getChiSoDienCu() != null
                ? req.getChiSoDienCu()
                : (previous != null ? previous.getChiSoDienMoi() : null));
        invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        invoice.setGiaDien(InvoicePricingEngine.resolveElectricPrice(contract, previous, req));
        invoice.setChiSoNuocCu(req.getChiSoNuocCu() != null
                ? req.getChiSoNuocCu()
                : (previous != null ? previous.getChiSoNuocMoi() : null));
        invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        invoice.setGiaNuoc(InvoicePricingEngine.resolveWaterPrice(contract, previous, req));
        invoice.setKieuTinhNuoc(InvoicePricingEngine.resolveWaterCalc(contract, previous, req));
        invoice.setTienPhong(InvoicePricingEngine.resolveRoomPrice(contract, previous));
        invoice.setHanThanhToan(req.getHanThanhToan());
        invoice.setTrangThai(InvoiceStatus.CHUA_THANH_TOAN);
        invoice.setGhiChu(req.getGhiChu());
        invoice.setNguoiTao(nguoiTao);
        invoice.setNgayTao(LocalDateTime.now());
        invoice.setNgaySua(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);

        // Build line items from contract (room / electric / water / services) and the request.
        List<com.example.rental.dto.InvoiceServiceItemRequest> merged = new ArrayList<>();
        for (com.example.rental.model.InvoiceServiceItem it
                : InvoicePricingEngine.buildItems(saved, req, contract, previous)) {
            com.example.rental.dto.InvoiceServiceItemRequest r = new com.example.rental.dto.InvoiceServiceItemRequest();
            r.setTenDichVu(it.getTenDichVu());
            r.setKieuTinh(it.getKieuTinh());
            r.setSoLuong(it.getSoLuong());
            r.setDonGia(it.getDonGia());
            r.setLaTuHopDong(it.getLaTuHopDong());
            merged.add(r);
        }
        syncServiceItems(saved, merged);

        // Total = sum of line items. Single source of truth.
        BigDecimal total = BigDecimal.ZERO;
        if (saved.getDanhSachDichVu() != null) {
            for (com.example.rental.model.InvoiceServiceItem it : saved.getDanhSachDichVu()) {
                if (it.getThanhTien() != null) total = total.add(it.getThanhTien());
            }
        }
        saved.setTongTien(total);
        Invoice result = invoiceRepository.save(saved);

        sendInvoiceNotification(result);
        notificationService.notifyInvoiceCreated(nguoiTao, result.getId());
        return result;
    }

    @Override
    @Transactional
    public Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception {
        return updateInvoice(id, req, null);
    }

    @Override
    @Transactional
    public Invoice updateInvoice(Long id, InvoiceRequest req, User nguoiSua) throws Exception {
        Invoice invoice = findById(id);

        if (invoice.getTrangThai() != InvoiceStatus.CHUA_THANH_TOAN) {
            throw new Exception("Hoa don da thanh toan, khong the cap nhat");
        }

        Contract contract = invoice.getHopDong();
        Invoice previous = findLatestByHopDongId(contract.getId());

        if (req.getKyHoaDon() != null) invoice.setKyHoaDon(req.getKyHoaDon());
        if (req.getChiSoDienCu() != null) invoice.setChiSoDienCu(req.getChiSoDienCu());
        if (req.getChiSoDienMoi() != null) invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        invoice.setGiaDien(InvoicePricingEngine.resolveElectricPrice(contract, previous, req));
        if (req.getChiSoNuocCu() != null) invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        if (req.getChiSoNuocMoi() != null) invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        invoice.setGiaNuoc(InvoicePricingEngine.resolveWaterPrice(contract, previous, req));
        invoice.setKieuTinhNuoc(InvoicePricingEngine.resolveWaterCalc(contract, previous, req));
        invoice.setTienPhong(req.getTienPhong() != null
                ? req.getTienPhong()
                : InvoicePricingEngine.resolveRoomPrice(contract, previous));
        if (req.getHanThanhToan() != null) invoice.setHanThanhToan(req.getHanThanhToan());
        if (req.getTrangThai() != null) invoice.setTrangThai(req.getTrangThai());
        if (req.getGhiChu() != null) invoice.setGhiChu(req.getGhiChu());
        invoice.setNgaySua(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);

        // Rebuild line items so totals stay consistent with the new meter readings.
        List<com.example.rental.dto.InvoiceServiceItemRequest> merged = new ArrayList<>();
        for (com.example.rental.model.InvoiceServiceItem it
                : InvoicePricingEngine.buildItems(saved, req, contract, previous)) {
            com.example.rental.dto.InvoiceServiceItemRequest r = new com.example.rental.dto.InvoiceServiceItemRequest();
            r.setTenDichVu(it.getTenDichVu());
            r.setKieuTinh(it.getKieuTinh());
            r.setSoLuong(it.getSoLuong());
            r.setDonGia(it.getDonGia());
            r.setLaTuHopDong(it.getLaTuHopDong());
            merged.add(r);
        }
        syncServiceItems(saved, merged);

        BigDecimal total = BigDecimal.ZERO;
        if (saved.getDanhSachDichVu() != null) {
            for (com.example.rental.model.InvoiceServiceItem it : saved.getDanhSachDichVu()) {
                if (it.getThanhTien() != null) total = total.add(it.getThanhTien());
            }
        }
        saved.setTongTien(total);
        return invoiceRepository.save(saved);
    }

    private void syncServiceItems(Invoice invoice, List<InvoiceServiceItemRequest> requests) {
        if (requests == null) return;
        invoice.getDanhSachDichVu().clear();
        invoiceServiceItemRepository.flush();

        for (InvoiceServiceItemRequest req : requests) {
            if (req.getTenDichVu() == null || req.getTenDichVu().trim().isEmpty()) {
                continue;
            }
            BigDecimal soLuong = req.getSoLuong() != null ? req.getSoLuong() : BigDecimal.ONE;
            BigDecimal donGia = req.getDonGia() != null ? req.getDonGia() : BigDecimal.ZERO;
            BigDecimal thanhTien = soLuong.multiply(donGia).setScale(0, RoundingMode.HALF_UP);

            InvoiceServiceItem item = new InvoiceServiceItem();
            item.setHoaDon(invoice);
            item.setTenDichVu(req.getTenDichVu());
            item.setKieuTinh(req.getKieuTinh() != null ? req.getKieuTinh() : "Theo phong");
            item.setSoLuong(soLuong);
            item.setDonGia(donGia);
            item.setThanhTien(thanhTien);
            item.setLaTuHopDong(req.getLaTuHopDong() == null ? Boolean.TRUE : req.getLaTuHopDong());
            invoice.getDanhSachDichVu().add(item);
        }
        invoiceServiceItemRepository.saveAll(invoice.getDanhSachDichVu());
    }

    @Override
    public Invoice findById(Long id) throws Exception {
        return findById(id, null);
    }

    @Override
    public Invoice findById(Long id, User currentUser) throws Exception {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay hoa don voi id " + id));
        if (currentUser != null && !userService.isAdmin(currentUser)) {
            if (!userService.canAccessInvoice(currentUser, id)) {
                throw new Exception("Ban khong co quyen truy cap hoa don nay");
            }
        }
        return invoice;
    }

    @Override
    public Invoice findByMaHoaDon(String maHoaDon) throws Exception {
        Invoice invoice = invoiceRepository.findByMaHoaDon(maHoaDon);
        if (invoice == null) {
            throw new Exception("Khong tim thay hoa don voi ma " + maHoaDon);
        }
        return invoice;
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    @Override
    public List<Invoice> findAll(User currentUser) {
        if (currentUser == null || userService.isAdmin(currentUser)) {
            return findAll();
        }
        Set<Long> allowedMotelIds = userService.getAssignedMotelIds(currentUser);
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getHopDong() != null
                        && i.getHopDong().getPhongTro() != null
                        && i.getHopDong().getPhongTro().getNhaTro() != null
                        && allowedMotelIds.contains(i.getHopDong().getPhongTro().getNhaTro().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findByHopDongId(Long hopDongId) {
        return findByHopDongId(hopDongId, null);
    }

    @Override
    public List<Invoice> findByHopDongId(Long hopDongId, User currentUser) {
        List<Invoice> all = findAll(currentUser);
        return all.stream()
                .filter(i -> i.getHopDong() != null && i.getHopDong().getId().equals(hopDongId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findByTrangThai(InvoiceStatus trangThai) {
        return findByTrangThai(trangThai, null);
    }

    @Override
    public List<Invoice> findByTrangThai(InvoiceStatus trangThai, User currentUser) {
        return findAll(currentUser).stream()
                .filter(i -> trangThai == null || i.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> search(String keyword, InvoiceStatus trangThai) {
        return search(keyword, trangThai, null);
    }

    @Override
    public List<Invoice> search(String keyword, InvoiceStatus trangThai, User currentUser) {
        List<Invoice> invoices = findAll(currentUser);
        return invoices.stream()
                .filter(i -> keyword == null || keyword.isBlank()
                        || (i.getMaHoaDon() != null && i.getMaHoaDon().toLowerCase().contains(keyword.toLowerCase())))
                .filter(i -> trangThai == null || i.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInvoice(Long id) throws Exception {
        deleteInvoice(id, null);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id, User currentUser) throws Exception {
        Invoice invoice = findById(id, currentUser);

        if (invoice.getTrangThai() != InvoiceStatus.CHUA_THANH_TOAN) {
            throw new Exception("Hoa don da thanh toan, khong the xoa");
        }

        invoiceServiceItemRepository.deleteByHoaDonId(invoice.getId());
        invoiceRepository.delete(invoice);
    }

    @Override
    @Transactional
    public Invoice markAsPaid(Long id) throws Exception {
        return markAsPaid(id, null);
    }

    @Override
    @Transactional
    public Invoice markAsPaid(Long id, User nguoiThanhToan) throws Exception {
        Invoice invoice = findById(id);
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new Exception("Hoa don da duoc thanh toan truoc do");
        }

        boolean hasExistingCash = transactionRepository.findByHoaDonId(invoice.getId()).stream()
                .anyMatch(t -> t.getHinhThucTT() == PaymentMethod.TIEN_MAT);
        if (!hasExistingCash) {
            Transaction tx = new Transaction();
            tx.setMaGiaoDich(generateMaGiaoDich());
            tx.setHoaDon(invoice);
            tx.setSoTien(invoice.getTongTien() != null ? invoice.getTongTien() : BigDecimal.ZERO);
            tx.setHinhThucTT(PaymentMethod.TIEN_MAT);
            tx.setMaCongTT("CASH");
            tx.setTrangThai(PaymentStatus.THANH_CONG);
            LocalDateTime now = LocalDateTime.now();
            tx.setNgayThanhToan(now);
            tx.setNgayTao(now);
            tx.setGhiChu("Thu tien mat hoa don " + invoice.getMaHoaDon());
            transactionRepository.save(tx);
        }

        invoice.setTrangThai(InvoiceStatus.DA_THANH_TOAN);
        invoice.setNgaySua(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);
        notificationService.notifyInvoicePaid(nguoiThanhToan, saved.getId(), "tien mat");
        return saved;
    }

    private String generateMaGiaoDich() {
        long count = transactionRepository.count() + 1;
        return "GD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%04d", count);
    }

    @Override
    public Invoice findLatestByHopDongId(Long hopDongId) {
        if (hopDongId == null) return null;
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getHopDong() != null && i.getHopDong().getId().equals(hopDongId))
                .max((a, b) -> {
                    if (a.getKyHoaDon() == null && b.getKyHoaDon() == null) return 0;
                    if (a.getKyHoaDon() == null) return -1;
                    if (b.getKyHoaDon() == null) return 1;
                    return a.getKyHoaDon().compareTo(b.getKyHoaDon());
                })
                .orElse(null);
    }

    public BigDecimal computeElectricAmount(Contract contract, InvoiceRequest req) {
        return InvoicePricingEngine.computeElectric(req, contract);
    }

    public BigDecimal computeWaterAmount(Contract contract, InvoiceRequest req) {
        return InvoicePricingEngine.computeWater(req, contract);
    }

    public BigDecimal computeServiceAmount(InvoiceRequest req) {
        return InvoicePricingEngine.computeServices(req);
    }

    public BigDecimal computeTotal(Contract contract, InvoiceRequest req) {
        return InvoicePricingEngine.computeTotal(null, req, contract);
    }

    public int countPeopleByRoomId(Long roomId) {
        if (roomId == null) return 1;
        try {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null) return 1;
            if (room.getSoNguoi() != null && room.getSoNguoi() > 0) {
                return room.getSoNguoi();
            }
        } catch (Exception ignored) {
        }
        try {
            List<Tenant> tenants = tenantRepository.findByPhongTroId(roomId);
            if (!tenants.isEmpty()) {
                int total = 0;
                for (Tenant t : tenants) {
                    total += 1;
                    if (t.getDanhSachNguoiOCung() != null) {
                        total += t.getDanhSachNguoiOCung().size();
                    }
                }
                return Math.max(total, 1);
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private void sendInvoiceNotification(Invoice invoice) {
        try {
            Contract contract = invoice.getHopDong();
            Tenant tenant = contract != null ? contract.getKhachThue() : null;
            Room room = contract != null ? contract.getPhongTro() : null;

            if (tenant == null || tenant.getEmail() == null || tenant.getEmail().isBlank()) {
                log.warn("Khong the gui email: khach thue hoac email trong");
                return;
            }

            String propertyName = (room != null && room.getNhaTro() != null) ? room.getNhaTro().getTenTro() : "-";
            String roomLabel = room != null ? room.getMaPhong() : "-";

            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("tenant_name", tenant.getHoTen() != null ? tenant.getHoTen() : "Quy khach");
            vars.put("room", roomLabel);
            vars.put("property", propertyName);
            vars.put("month", invoice.getKyHoaDon() != null ? invoice.getKyHoaDon().format(MONTH_FMT) : "-");
            vars.put("due_date", invoice.getHanThanhToan() != null ? invoice.getHanThanhToan().format(DATE_FMT) : "-");
            vars.put("amount", invoice.getTongTien() != null ? MONEY_FMT.format(invoice.getTongTien()) : "0");
            vars.put("invoice_code", invoice.getMaHoaDon() != null ? invoice.getMaHoaDon() : "");
            vars.put("invoice_url", vnPayConfig.getPublicBaseUrl() + "/api/invoices/" + invoice.getId() + "/pdf");
            vars.put("payment_url", vnPayConfig.getPublicBaseUrl() + "/pay/" + invoice.getMaHoaDon());

            emailTemplateService.sendWithTemplate(TEMPLATE_PAYMENT_REMINDER, tenant.getEmail(), vars);
        } catch (Exception e) {
            log.error("Loi khi gui thong bao hoa don: {}", e.getMessage());
        }
    }
}
