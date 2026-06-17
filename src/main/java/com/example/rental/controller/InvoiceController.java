package com.example.rental.controller;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.model.Invoice;
import com.example.rental.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody InvoiceRequest req) throws Exception {
        Invoice invoice = invoiceService.createInvoice(req);
        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable Long id,
            @RequestBody InvoiceRequest req) throws Exception {
        Invoice invoice = invoiceService.updateInvoice(id, req);
        return ResponseEntity.ok(invoice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteInvoice(@PathVariable Long id) throws Exception {
        invoiceService.deleteInvoice(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa hoa don thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) throws Exception {
        Invoice invoice = invoiceService.findById(id);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        List<Invoice> invoices = invoiceService.findAll();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/contract/{hopDongId}")
    public ResponseEntity<List<Invoice>> getInvoicesByContract(@PathVariable Long hopDongId) {
        List<Invoice> invoices = invoiceService.findByHopDongId(hopDongId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Invoice>> searchInvoices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) InvoiceStatus trangThai) {
        List<Invoice> invoices = invoiceService.search(keyword, trangThai);
        return ResponseEntity.ok(invoices);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Invoice> markAsPaid(@PathVariable Long id) throws Exception {
        Invoice invoice = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(invoice);
    }
}
