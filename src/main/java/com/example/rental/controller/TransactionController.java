package com.example.rental.controller;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Transaction;
import com.example.rental.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

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

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.findAll();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/invoice/{hoaDonId}")
    public ResponseEntity<List<Transaction>> getTransactionsByInvoice(@PathVariable Long hoaDonId) {
        List<Transaction> transactions = transactionService.findByInvoiceId(hoaDonId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Transaction>> searchTransactions(
            @RequestParam(required = false) String maGiaoDich,
            @RequestParam(required = false) Long hoaDonId,
            @RequestParam(required = false) PaymentStatus trangThai,
            @RequestParam(required = false) PaymentMethod hinhThucTT,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime denNgay) {
        List<Transaction> transactions = transactionService.search(
                maGiaoDich, hoaDonId, trangThai, hinhThucTT, tuNgay, denNgay);
        return ResponseEntity.ok(transactions);
    }
}
