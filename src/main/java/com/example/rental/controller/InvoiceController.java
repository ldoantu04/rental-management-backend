package com.example.rental.controller;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.model.Invoice;
import com.example.rental.model.User;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.UserService;
import com.example.rental.service.utils.InvoicePdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(
            @RequestBody InvoiceRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Invoice invoice = invoiceService.createInvoice(req, user);
        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable Long id,
            @RequestBody InvoiceRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Invoice invoice = invoiceService.updateInvoice(id, req, user);
        return ResponseEntity.ok(invoice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteInvoice(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        invoiceService.deleteInvoice(id, user);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa hoa don thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        Invoice invoice = invoiceService.findById(id, user);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/code/{maHoaDon}")
    public ResponseEntity<Invoice> getInvoiceByCode(@PathVariable String maHoaDon) throws Exception {
        Invoice invoice = invoiceService.findByMaHoaDon(maHoaDon);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Invoice> invoices = invoiceService.findAll(user);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/contract/{hopDongId}")
    public ResponseEntity<List<Invoice>> getInvoicesByContract(
            @PathVariable Long hopDongId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Invoice> invoices = invoiceService.findByHopDongId(hopDongId, user);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/contract/{hopDongId}/latest")
    public ResponseEntity<Invoice> getLatestInvoiceByContract(@PathVariable Long hopDongId) {
        Invoice invoice = invoiceService.findLatestByHopDongId(hopDongId);
        if (invoice == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Invoice>> searchInvoices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) InvoiceStatus trangThai,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Invoice> invoices = invoiceService.search(keyword, trangThai, user);
        return ResponseEntity.ok(invoices);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Invoice> markAsPaid(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Invoice invoice = invoiceService.markAsPaid(id, user);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        Invoice invoice = invoiceService.findById(id, user);
        byte[] pdf = invoicePdfService.generate(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String safeCode = (invoice.getMaHoaDon() == null ? "hoa-don" : invoice.getMaHoaDon()).replaceAll("[^\\w\\-]", "_");
        headers.setContentDispositionFormData("attachment", safeCode + ".pdf");
        headers.setCacheControl("must-revalidate, no-store");
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
