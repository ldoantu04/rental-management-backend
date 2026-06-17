package com.example.rental.service.impl;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.InvoiceServiceItemRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.model.Transaction;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.InvoiceServiceItemRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceServiceItemRepository invoiceServiceItemRepository;
    private final ContractRepository contractRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Invoice createInvoice(InvoiceRequest req) throws Exception {
        Contract contract = null;
        if (req.getMaHopDong() != null) {
            contract = contractRepository.findById(req.getMaHopDong())
                    .orElseThrow(() -> new Exception("Khong tim thay hop dong voi id " + req.getMaHopDong()));
        }

        Invoice invoice = new Invoice();
        invoice.setHopDong(contract);
        invoice.setKyHoaDon(req.getKyHoaDon());
        invoice.setChiSoDienCu(req.getChiSoDienCu());
        invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        invoice.setGiaDien(req.getGiaDien() != null ? req.getGiaDien() : BigDecimal.ZERO);
        invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        invoice.setGiaNuoc(req.getGiaNuoc() != null ? req.getGiaNuoc() : BigDecimal.ZERO);
        invoice.setTienPhong(req.getTienPhong() != null ? req.getTienPhong() : BigDecimal.ZERO);
        invoice.setHanThanhToan(req.getHanThanhToan());
        invoice.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : InvoiceStatus.CHUA_THANH_TOAN);
        invoice.setGhiChu(req.getGhiChu());
        invoice.setNgayTao(LocalDateTime.now());
        invoice.setNgaySua(LocalDateTime.now());

        BigDecimal tongTien = calculateTongTien(invoice, req.getDanhSachDichVu());
        invoice.setTongTien(tongTien);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        attachDichVu(savedInvoice, req.getDanhSachDichVu());
        return savedInvoice;
    }

    @Override
    @Transactional
    public Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception {
        Invoice invoice = findById(id);

        if (req.getMaHopDong() != null) {
            Contract contract = contractRepository.findById(req.getMaHopDong())
                    .orElseThrow(() -> new Exception("Khong tim thay hop dong voi id " + req.getMaHopDong()));
            invoice.setHopDong(contract);
        }
        if (req.getKyHoaDon() != null) {
            invoice.setKyHoaDon(req.getKyHoaDon());
        }
        if (req.getChiSoDienCu() != null) {
            invoice.setChiSoDienCu(req.getChiSoDienCu());
        }
        if (req.getChiSoDienMoi() != null) {
            invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        }
        if (req.getGiaDien() != null) {
            invoice.setGiaDien(req.getGiaDien());
        }
        if (req.getChiSoNuocCu() != null) {
            invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        }
        if (req.getChiSoNuocMoi() != null) {
            invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        }
        if (req.getGiaNuoc() != null) {
            invoice.setGiaNuoc(req.getGiaNuoc());
        }
        if (req.getTienPhong() != null) {
            invoice.setTienPhong(req.getTienPhong());
        }
        if (req.getHanThanhToan() != null) {
            invoice.setHanThanhToan(req.getHanThanhToan());
        }
        if (req.getTrangThai() != null) {
            invoice.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            invoice.setGhiChu(req.getGhiChu());
        }
        invoice.setNgaySua(LocalDateTime.now());

        BigDecimal tongTien = calculateTongTien(invoice, req.getDanhSachDichVu());
        invoice.setTongTien(tongTien);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        if (req.getDanhSachDichVu() != null) {
            invoiceServiceItemRepository.deleteByHoaDonId(id);
            attachDichVu(savedInvoice, req.getDanhSachDichVu());
        }
        return savedInvoice;
    }

    private BigDecimal calculateTongTien(Invoice invoice, List<InvoiceServiceItemRequest> dichVus) {
        BigDecimal tong = BigDecimal.ZERO;

        if (invoice.getTienPhong() != null) {
            tong = tong.add(invoice.getTienPhong());
        }

        if (invoice.getChiSoDienMoi() != null && invoice.getChiSoDienCu() != null && invoice.getGiaDien() != null) {
            int soDien = Math.max(invoice.getChiSoDienMoi() - invoice.getChiSoDienCu(), 0);
            tong = tong.add(invoice.getGiaDien().multiply(BigDecimal.valueOf(soDien)));
        }

        if (invoice.getChiSoNuocMoi() != null && invoice.getChiSoNuocCu() != null && invoice.getGiaNuoc() != null) {
            int soNuoc = Math.max(invoice.getChiSoNuocMoi() - invoice.getChiSoNuocCu(), 0);
            tong = tong.add(invoice.getGiaNuoc().multiply(BigDecimal.valueOf(soNuoc)));
        }

        if (dichVus != null) {
            for (InvoiceServiceItemRequest dv : dichVus) {
                if (dv.getSoLuong() != null && dv.getDonGia() != null) {
                    tong = tong.add(dv.getSoLuong().multiply(dv.getDonGia()));
                }
            }
        }

        return tong;
    }

    private void attachDichVu(Invoice invoice, List<InvoiceServiceItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (InvoiceServiceItemRequest req : requests) {
            if (req.getTenDichVu() == null || req.getTenDichVu().trim().isEmpty()) {
                continue;
            }
            InvoiceServiceItem item = new InvoiceServiceItem();
            item.setHoaDon(invoice);
            item.setTenDichVu(req.getTenDichVu());
            item.setSoLuong(req.getSoLuong() != null ? req.getSoLuong() : BigDecimal.ONE);
            item.setDonGia(req.getDonGia() != null ? req.getDonGia() : BigDecimal.ZERO);
            item.setThanhTien(item.getSoLuong().multiply(item.getDonGia()));
            invoiceServiceItemRepository.save(item);
        }
    }

    @Override
    public void deleteInvoice(Long id) throws Exception {
        Invoice invoice = findById(id);
        invoiceServiceItemRepository.deleteByHoaDonId(id);
        invoiceRepository.delete(invoice);
    }

    @Override
    public Invoice findById(Long id) throws Exception {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay hoa don voi id " + id));
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getNgayTao() == null) return 1;
                    if (b.getNgayTao() == null) return -1;
                    return b.getNgayTao().compareTo(a.getNgayTao());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findByHopDongId(Long hopDongId) {
        return invoiceRepository.findAll().stream()
                .filter(inv -> inv.getHopDong() != null && inv.getHopDong().getId().equals(hopDongId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> search(String keyword, InvoiceStatus trangThai) {
        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .filter(inv -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String kw = keyword.toLowerCase().trim();
                    if (inv.getMaHoaDon() != null && inv.getMaHoaDon().toLowerCase().contains(kw)) return true;
                    if (inv.getHopDong() != null && inv.getHopDong().getKhachThue() != null
                            && inv.getHopDong().getKhachThue().getHoTen() != null
                            && inv.getHopDong().getKhachThue().getHoTen().toLowerCase().contains(kw)) return true;
                    if (inv.getHopDong() != null && inv.getHopDong().getPhongTro() != null
                            && inv.getHopDong().getPhongTro().getMaPhong() != null
                            && inv.getHopDong().getPhongTro().getMaPhong().toLowerCase().contains(kw)) return true;
                    return false;
                })
                .filter(inv -> trangThai == null || inv.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Invoice markAsPaid(Long id) throws Exception {
        Invoice invoice = findById(id);
        invoice.setTrangThai(InvoiceStatus.DA_THANH_TOAN);
        invoice.setNgaySua(LocalDateTime.now());
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Tu dong tao giao dich thanh toan tien mat
        Transaction transaction = new Transaction();
        long count = transactionRepository.count();
        transaction.setMaGiaoDich("GD" + String.format("%03d", count + 1));
        transaction.setHoaDon(savedInvoice);
        transaction.setSoTien(savedInvoice.getTongTien() != null ? savedInvoice.getTongTien() : BigDecimal.ZERO);
        transaction.setHinhThucTT(PaymentMethod.TIEN_MAT);
        transaction.setTrangThai(PaymentStatus.THANH_CONG);
        transaction.setNgayThanhToan(LocalDateTime.now());
        transaction.setGhiChu("Thanh toan tien mat cho hoa don " + savedInvoice.getMaHoaDon());
        transaction.setNgayTao(LocalDateTime.now());
        transactionRepository.save(transaction);

        return savedInvoice;
    }
}
