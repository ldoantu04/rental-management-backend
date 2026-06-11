package com.example.rental.service.impl;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Transaction;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public Transaction findById(Long id) throws Exception {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay giao dich voi id " + id));
    }

    @Override
    public Transaction findByMaGiaoDich(String maGiaoDich) throws Exception {
        Transaction transaction = transactionRepository.findByMaGiaoDich(maGiaoDich);
        if (transaction == null) {
            throw new Exception("Khong tim thay giao dich voi ma " + maGiaoDich);
        }
        return transaction;
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAllByOrderByNgayTaoDesc();
    }

    @Override
    public List<Transaction> findByInvoiceId(Long hoaDonId) {
        return transactionRepository.findByHoaDonId(hoaDonId);
    }

    @Override
    public List<Transaction> search(String maGiaoDich, Long hoaDonId, PaymentStatus trangThai,
                                    PaymentMethod hinhThucTT, LocalDateTime tuNgay, LocalDateTime denNgay) {
        List<Transaction> transactions = transactionRepository.findAll();

        return transactions.stream()
                .filter(t -> maGiaoDich == null || t.getMaGiaoDich().toLowerCase().contains(maGiaoDich.toLowerCase()))
                .filter(t -> hoaDonId == null || (t.getHoaDon() != null && t.getHoaDon().getId().equals(hoaDonId)))
                .filter(t -> trangThai == null || t.getTrangThai() == trangThai)
                .filter(t -> hinhThucTT == null || t.getHinhThucTT() == hinhThucTT)
                .filter(t -> tuNgay == null || (t.getNgayTao() != null && !t.getNgayTao().isBefore(tuNgay)))
                .filter(t -> denNgay == null || (t.getNgayTao() != null && !t.getNgayTao().isAfter(denNgay)))
                .collect(Collectors.toList());
    }
}
