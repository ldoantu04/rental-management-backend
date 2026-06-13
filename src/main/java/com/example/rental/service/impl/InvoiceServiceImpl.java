package com.example.rental.service.impl;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.InvoiceServiceItemRepository;
import com.example.rental.service.ContractService;
import com.example.rental.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceServiceItemRepository invoiceServiceItemRepository;
    private final ContractService contractService;

    @Override
    @Transactional
    public Invoice createInvoice(InvoiceRequest req) throws Exception {
        Contract contract = contractService.findById(req.getMaHopDong());

        Invoice invoice = new Invoice();
        String nextCode = "INV" + String.format("%03d", invoiceRepository.count() + 1);
        invoice.setMaHoaDon(nextCode);
        invoice.setHopDong(contract);
        invoice.setKyHoaDon(req.getKyHoaDon());
        invoice.setChiSoDienCu(req.getChiSoDienCu());
        invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        invoice.setGiaDien(req.getGiaDien());
        invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        invoice.setGiaNuoc(req.getGiaNuoc());
        invoice.setTienPhong(req.getTienPhong() != null ? req.getTienPhong() : contract.getGiaThue());
        invoice.setHanThanhToan(req.getHanThanhToan());
        invoice.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : InvoiceStatus.CHUA_THANH_TOAN);
        invoice.setGhiChu(req.getGhiChu());
        invoice.setNgayTao(LocalDateTime.now());
        invoice.setNgaySua(LocalDateTime.now());

        // Calculate total
        BigDecimal total = calculateTotal(invoice, req);
        invoice.setTongTien(total);

        Invoice saved = invoiceRepository.save(invoice);

        // Save service items
        if (req.getDichVu() != null) {
            for (InvoiceRequest.InvoiceServiceItemRequest itemReq : req.getDichVu()) {
                InvoiceServiceItem item = new InvoiceServiceItem();
                item.setHoaDon(saved);
                item.setTenDichVu(itemReq.getTenDichVu());
                item.setSoLuong(itemReq.getSoLuong() != null ? itemReq.getSoLuong() : BigDecimal.ONE);
                item.setDonGia(itemReq.getDonGia());
                item.setThanhTien(item.getSoLuong().multiply(item.getDonGia()));
                invoiceServiceItemRepository.save(item);
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception {
        Invoice invoice = findById(id);

        if (req.getMaHopDong() != null) {
            invoice.setHopDong(contractService.findById(req.getMaHopDong()));
        }
        if (req.getKyHoaDon() != null) invoice.setKyHoaDon(req.getKyHoaDon());
        if (req.getChiSoDienCu() != null) invoice.setChiSoDienCu(req.getChiSoDienCu());
        if (req.getChiSoDienMoi() != null) invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        if (req.getGiaDien() != null) invoice.setGiaDien(req.getGiaDien());
        if (req.getChiSoNuocCu() != null) invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        if (req.getChiSoNuocMoi() != null) invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        if (req.getGiaNuoc() != null) invoice.setGiaNuoc(req.getGiaNuoc());
        if (req.getTienPhong() != null) invoice.setTienPhong(req.getTienPhong());
        if (req.getHanThanhToan() != null) invoice.setHanThanhToan(req.getHanThanhToan());
        if (req.getTrangThai() != null) invoice.setTrangThai(req.getTrangThai());
        if (req.getGhiChu() != null) invoice.setGhiChu(req.getGhiChu());
        invoice.setNgaySua(LocalDateTime.now());

        BigDecimal total = calculateTotal(invoice, req);
        invoice.setTongTien(total);

        // Update service items
        if (req.getDichVu() != null) {
            invoiceServiceItemRepository.deleteByHoaDonId(id);
            for (InvoiceRequest.InvoiceServiceItemRequest itemReq : req.getDichVu()) {
                InvoiceServiceItem item = new InvoiceServiceItem();
                item.setHoaDon(invoice);
                item.setTenDichVu(itemReq.getTenDichVu());
                item.setSoLuong(itemReq.getSoLuong() != null ? itemReq.getSoLuong() : BigDecimal.ONE);
                item.setDonGia(itemReq.getDonGia());
                item.setThanhTien(item.getSoLuong().multiply(item.getDonGia()));
                invoiceServiceItemRepository.save(item);
            }
        }

        return invoiceRepository.save(invoice);
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
                .orElseThrow(() -> new Exception("Khong tim thay hoa don voi id: " + id));
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice markAsPaid(Long id) throws Exception {
        Invoice invoice = findById(id);
        invoice.setTrangThai(InvoiceStatus.DA_THANH_TOAN);
        invoice.setNgaySua(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    private BigDecimal calculateTotal(Invoice invoice, InvoiceRequest req) {
        BigDecimal total = invoice.getTienPhong() != null ? invoice.getTienPhong() : BigDecimal.ZERO;

        // Electricity
        int electricUsage = Math.max((invoice.getChiSoDienMoi() != null ? invoice.getChiSoDienMoi() : 0) -
                (invoice.getChiSoDienCu() != null ? invoice.getChiSoDienCu() : 0), 0);
        BigDecimal electricCost = invoice.getGiaDien() != null ?
                invoice.getGiaDien().multiply(BigDecimal.valueOf(electricUsage)) : BigDecimal.ZERO;
        total = total.add(electricCost);

        // Water
        int waterUsage = Math.max((invoice.getChiSoNuocMoi() != null ? invoice.getChiSoNuocMoi() : 0) -
                (invoice.getChiSoNuocCu() != null ? invoice.getChiSoNuocCu() : 0), 0);
        BigDecimal waterCost = invoice.getGiaNuoc() != null ?
                invoice.getGiaNuoc().multiply(BigDecimal.valueOf(waterUsage)) : BigDecimal.ZERO;
        total = total.add(waterCost);

        // Services
        if (req.getDichVu() != null) {
            for (InvoiceRequest.InvoiceServiceItemRequest item : req.getDichVu()) {
                BigDecimal qty = item.getSoLuong() != null ? item.getSoLuong() : BigDecimal.ONE;
                BigDecimal price = item.getDonGia() != null ? item.getDonGia() : BigDecimal.ZERO;
                total = total.add(qty.multiply(price));
            }
        }

        return total;
    }
}
