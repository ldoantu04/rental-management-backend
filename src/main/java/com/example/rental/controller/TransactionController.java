package com.example.rental.controller;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.model.Transaction;
import com.example.rental.service.TransactionService;
import com.example.rental.service.utils.TransactionExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionExcelService transactionExcelService;

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.findAll();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) throws Exception {
        Transaction transaction = transactionService.findById(id);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/code/{maGiaoDich}")
    public ResponseEntity<Transaction> getTransactionByCode(@PathVariable String maGiaoDich) throws Exception {
        Transaction transaction = transactionService.findByMaGiaoDich(maGiaoDich);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/invoice/{hoaDonId}")
    public ResponseEntity<List<Transaction>> getTransactionsByInvoice(@PathVariable Long hoaDonId) {
        List<Transaction> transactions = transactionService.findByInvoiceId(hoaDonId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Transaction>> searchTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String maHoaDon,
            @RequestParam(required = false) String tenKhachThue,
            @RequestParam(required = false) PaymentStatus trangThai,
            @RequestParam(required = false) PaymentMethod hinhThucTT,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime denNgay) {
        List<Transaction> transactions = transactionService.search(
                keyword, maHoaDon, tenKhachThue, trangThai, hinhThucTT, tuNgay, denNgay);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String maHoaDon,
            @RequestParam(required = false) String tenKhachThue,
            @RequestParam(required = false) PaymentStatus trangThai,
            @RequestParam(required = false) PaymentMethod hinhThucTT,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime denNgay) throws Exception {
        List<Transaction> transactions = transactionService.filterForExport(
                keyword, maHoaDon, tenKhachThue, trangThai, hinhThucTT, tuNgay, denNgay);
        byte[] data = transactionExcelService.exportTransactions(transactions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "danh-sach-giao-dich.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTransaction(@PathVariable Long id) throws Exception {
        transactionService.deleteTransaction(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa giao dich thanh cong");
        return ResponseEntity.ok(res);
    }
}
