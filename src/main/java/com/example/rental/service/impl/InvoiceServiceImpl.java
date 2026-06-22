package com.example.rental.service.impl;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.InvoiceServiceItemRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.InvoiceServiceItemRepository;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.NotificationService;
import com.example.rental.service.SystemSettingService;
import com.example.rental.service.UserService;
import com.example.rental.service.utils.InvoicePricingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final InvoiceServiceItemRepository invoiceServiceItemRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final SystemSettingService systemSettingService;

    // ====================================================================
    //  Create
    // ====================================================================

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

        Invoice previous = findLatestByHopDongId(contract.getId());

        // Build the invoice header
        Invoice invoice = new Invoice();
        invoice.setMaHoaDon(generateMaHoaDon());
        invoice.setHopDong(contract);
        invoice.setKyHoaDon(req.getKyHoaDon());

        // Electric
        invoice.setChiSoDienCu(
                req.getChiSoDienCu() != null
                        ? req.getChiSoDienCu()
                        : (previous != null ? previous.getChiSoDienMoi() : null));
        invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        invoice.setGiaDien(InvoicePricingEngine.resolveElectricPrice(contract, previous, req));

        // Water
        invoice.setChiSoNuocCu(
                req.getChiSoNuocCu() != null
                        ? req.getChiSoNuocCu()
                        : (previous != null ? previous.getChiSoNuocMoi() : null));
        invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        invoice.setGiaNuoc(InvoicePricingEngine.resolveWaterPrice(contract, previous, req));
        invoice.setKieuTinhNuoc(InvoicePricingEngine.resolveWaterCalc(contract, previous, req));

        // Room
        invoice.setTienPhong(
                req.getTienPhong() != null
                        ? req.getTienPhong()
                        : InvoicePricingEngine.resolveRoomPrice(contract, previous));

        // Meta
        invoice.setHanThanhToan(req.getHanThanhToan() != null
                ? req.getHanThanhToan()
                : calculateDefaultDueDate(req.getKyHoaDon()));
        invoice.setTrangThai(InvoiceStatus.CHUA_THANH_TOAN);
        invoice.setPhiPhat(req.getPhiPhat() != null ? req.getPhiPhat() : BigDecimal.ZERO);
        invoice.setGhiChu(req.getGhiChu());
        invoice.setNguoiTao(nguoiTao);
        invoice.setNgayTao(LocalDateTime.now());
        invoice.setNgaySua(LocalDateTime.now());

        // Persist header first (needed for FK in line items)
        Invoice saved = invoiceRepository.save(invoice);

        // Build all line items from the engine (single source of truth)
        syncServiceItems(saved, buildItemRequests(saved, req, contract, previous));

        // Set total = sum of thanhTien + phiPhat
        saved.setTongTien(InvoicePricingEngine.computeTotal(saved));
        // Auto-calculate late penalty if past due date
        if (saved.getHanThanhToan() != null && saved.getHanThanhToan().isBefore(java.time.LocalDate.now())) {
            BigDecimal penaltyPct = systemSettingService.getLatePenaltyPercent();
            BigDecimal phiPhat = saved.getTongTien().multiply(penaltyPct)
                    .divide(new BigDecimal("100"), 0, java.math.RoundingMode.HALF_UP);
            saved.setPhiPhat(phiPhat);
            saved.setTongTien(saved.getTongTien().add(phiPhat));
        }
        Invoice result = invoiceRepository.save(saved);

        sendInvoiceNotification(result);
        notificationService.notifyInvoiceCreated(nguoiTao, result.getId());
        return result;
    }

    // ====================================================================
    //  Update
    // ====================================================================

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
        // Find the latest invoice before this one (not including itself) to get correct meter readings
        Invoice previous = invoiceRepository.findAll().stream()
                .filter(i -> i.getHopDong() != null
                        && i.getHopDong().getId().equals(contract.getId())
                        && i.getId() != null && !i.getId().equals(id)
                        && i.getKyHoaDon() != null)
                .max((a, b) -> {
                    if (a.getKyHoaDon() == null && b.getKyHoaDon() == null) return 0;
                    if (a.getKyHoaDon() == null) return -1;
                    if (b.getKyHoaDon() == null) return 1;
                    return b.getKyHoaDon().compareTo(a.getKyHoaDon());
                })
                .orElse(null);

        // Update header fields
        if (req.getKyHoaDon() != null) invoice.setKyHoaDon(req.getKyHoaDon());

        invoice.setChiSoDienCu(req.getChiSoDienCu() != null ? req.getChiSoDienCu() : invoice.getChiSoDienCu());
        invoice.setChiSoDienMoi(req.getChiSoDienMoi() != null ? req.getChiSoDienMoi() : invoice.getChiSoDienMoi());
        invoice.setGiaDien(InvoicePricingEngine.resolveElectricPrice(contract, previous, req));

        invoice.setChiSoNuocCu(req.getChiSoNuocCu() != null ? req.getChiSoNuocCu() : invoice.getChiSoNuocCu());
        invoice.setChiSoNuocMoi(req.getChiSoNuocMoi() != null ? req.getChiSoNuocMoi() : invoice.getChiSoNuocMoi());
        invoice.setGiaNuoc(InvoicePricingEngine.resolveWaterPrice(contract, previous, req));
        invoice.setKieuTinhNuoc(InvoicePricingEngine.resolveWaterCalc(contract, previous, req));

        invoice.setTienPhong(req.getTienPhong() != null
                ? req.getTienPhong()
                : InvoicePricingEngine.resolveRoomPrice(contract, previous));

        if (req.getHanThanhToan() != null) invoice.setHanThanhToan(req.getHanThanhToan());
        if (req.getTrangThai() != null) invoice.setTrangThai(req.getTrangThai());
        if (req.getPhiPhat() != null) invoice.setPhiPhat(req.getPhiPhat());
        if (req.getGhiChu() != null) invoice.setGhiChu(req.getGhiChu());
        invoice.setNgaySua(LocalDateTime.now());

        // Persist updated header
        Invoice saved = invoiceRepository.save(invoice);

        // Rebuild all line items from the engine (single source of truth)
        syncServiceItems(saved, buildItemRequests(saved, req, contract, previous));

        // Recompute total
        saved.setTongTien(InvoicePricingEngine.computeTotal(saved));
        // Auto-calculate late penalty if past due date
        if (saved.getHanThanhToan() != null && saved.getHanThanhToan().isBefore(java.time.LocalDate.now())) {
            BigDecimal penaltyPct = systemSettingService.getLatePenaltyPercent();
            BigDecimal phiPhat = saved.getTongTien().multiply(penaltyPct)
                    .divide(new BigDecimal("100"), 0, java.math.RoundingMode.HALF_UP);
            saved.setPhiPhat(phiPhat);
            saved.setTongTien(saved.getTongTien().add(phiPhat));
        }
        return invoiceRepository.save(saved);
    }

    // ====================================================================
    //  Sync line items
    // ====================================================================

    /**
     * Converts {@link InvoiceServiceItem} entities from the engine into
     * {@link InvoiceServiceItemRequest} DTOs, then persists them.
     */
    private List<InvoiceServiceItemRequest> buildItemRequests(Invoice invoice,
                                                              InvoiceRequest req,
                                                              Contract contract,
                                                              Invoice previous) {
        List<InvoiceServiceItemRequest> requests = new ArrayList<>();
        for (InvoiceServiceItem it : InvoicePricingEngine.buildItems(invoice, req, contract, previous)) {
            InvoiceServiceItemRequest r = new InvoiceServiceItemRequest();
            r.setTenDichVu(it.getTenDichVu());
            r.setKieuTinh(it.getKieuTinh());
            r.setSoLuong(it.getSoLuong());
            r.setDonGia(it.getDonGia());
            r.setThanhTien(it.getThanhTien());
            r.setLaTuHopDong(it.getLaTuHopDong());
            r.setLoaiDichVu(it.getLoaiDichVu());
            r.setChiSoDau(it.getChiSoDau());
            r.setChiSoCuoi(it.getChiSoCuoi());
            requests.add(r);
        }
        return requests;
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
            // Accept thanhTien from request (pre-computed by engine); compute if missing.
            BigDecimal thanhTien = req.getThanhTien();
            if (thanhTien == null) {
                thanhTien = soLuong.multiply(donGia).setScale(0, java.math.RoundingMode.HALF_UP);
            }

            InvoiceServiceItem item = new InvoiceServiceItem();
            item.setHoaDon(invoice);
            item.setTenDichVu(req.getTenDichVu());
            item.setKieuTinh(req.getKieuTinh() != null ? req.getKieuTinh() : "Theo phòng");
            item.setSoLuong(soLuong);
            item.setDonGia(donGia);
            item.setThanhTien(thanhTien);
            item.setLaTuHopDong(req.getLaTuHopDong() == null ? Boolean.TRUE : req.getLaTuHopDong());
            item.setLoaiDichVu(req.getLoaiDichVu() != null ? req.getLoaiDichVu() : InvoicePricingEngine.LOAI_DICH_VU);
            item.setChiSoDau(req.getChiSoDau());
            item.setChiSoCuoi(req.getChiSoCuoi());
            invoice.getDanhSachDichVu().add(item);
        }
        invoiceServiceItemRepository.saveAll(invoice.getDanhSachDichVu());
    }

    // ====================================================================
    //  Query
    // ====================================================================

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
                .toList();
    }

    @Override
    public List<Invoice> findByTrangThai(InvoiceStatus trangThai) {
        return invoiceRepository.findByTrangThai(trangThai);
    }

    @Override
    public List<Invoice> findByTrangThai(InvoiceStatus trangThai, User currentUser) {
        return findAll(currentUser).stream()
                .filter(i -> i.getTrangThai() == trangThai)
                .toList();
    }

    @Override
    public List<Invoice> search(String keyword, InvoiceStatus trangThai) {
        return search(keyword, trangThai, null);
    }

    @Override
    public List<Invoice> search(String keyword, InvoiceStatus trangThai, User currentUser) {
        List<Invoice> all = findAll(currentUser);
        if (trangThai != null) {
            all = all.stream().filter(i -> i.getTrangThai() == trangThai).toList();
        }
        if (keyword == null || keyword.isBlank()) return all;
        String kw = keyword.toLowerCase();
        return all.stream().filter(i -> {
            if (i.getMaHoaDon() != null && i.getMaHoaDon().toLowerCase().contains(kw)) return true;
            if (i.getHopDong() != null && i.getHopDong().getKhachThue() != null
                    && i.getHopDong().getKhachThue().getHoTen() != null
                    && i.getHopDong().getKhachThue().getHoTen().toLowerCase().contains(kw)) return true;
            return false;
        }).toList();
    }

    @Override
    public Invoice markAsPaid(Long id) throws Exception {
        return markAsPaid(id, null);
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
                .toList();
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
                    return b.getKyHoaDon().compareTo(a.getKyHoaDon());
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public Invoice markAsPaid(Long id, User nguoiThanhToan) throws Exception {
        Invoice invoice = findById(id);
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new Exception("Hoa don da duoc thanh toan");
        }
        invoice.setTrangThai(InvoiceStatus.DA_THANH_TOAN);
        invoice.setNgaySua(LocalDateTime.now());
        notificationService.notifyInvoicePaid(nguoiThanhToan, invoice.getId(), "TIEN_MAT");
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id) throws Exception {
        deleteInvoice(id, null);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id, User currentUser) throws Exception {
        Invoice invoice = findById(id, currentUser);
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new Exception("Hoa don da thanh toan, khong the xoa");
        }
        invoiceRepository.delete(invoice);
    }

    @Override
    public BigDecimal computeElectricAmount(Contract contract, InvoiceRequest req) {
        return InvoicePricingEngine.computeElectric(req, contract);
    }

    @Override
    public BigDecimal computeWaterAmount(Contract contract, InvoiceRequest req) {
        return InvoicePricingEngine.computeWater(req, contract);
    }

    @Override
    public BigDecimal computeServiceAmount(InvoiceRequest req) {
        return InvoicePricingEngine.computeServices(req);
    }

    @Override
    public BigDecimal computeTotal(Contract contract, InvoiceRequest req) {
        // For pre-save estimation only — actual total comes from InvoicePricingEngine.computeTotal
        return InvoicePricingEngine.computeTotal(new Invoice(), req, contract);
    }

    @Override
    @Transactional(readOnly = true)
    public int countPeopleByRoomId(Long roomId) {
        if (roomId == null) return 1;
        List<Contract> active = contractRepository.findByPhongTroIdAndTrangThai(
                roomId, com.example.rental.domain.ContractStatus.DANG_HIEU_LUC);
        if (active == null || active.isEmpty()) return 1;
        Contract contract = active.get(0);
        if (contract.getKhachThue() == null) return 1;
        int count = 1;
        if (contract.getKhachThue().getDanhSachNguoiOCung() != null) {
            count += contract.getKhachThue().getDanhSachNguoiOCung().size();
        }
        return count;
    }

    // ====================================================================
    //  Helpers
    // ====================================================================

    private String generateMaHoaDon() {
        long count = invoiceRepository.count();
        return "HD" + String.format("%05d", count + 1);
    }

    private LocalDate calculateDefaultDueDate(LocalDate kyHoaDon) {
        if (kyHoaDon == null) return LocalDate.now().plusDays(5);
        return kyHoaDon.plusDays(5);
    }

    private void sendInvoiceNotification(Invoice invoice) {
        // Implemented by NotificationService — called after invoice creation.
    }
}
