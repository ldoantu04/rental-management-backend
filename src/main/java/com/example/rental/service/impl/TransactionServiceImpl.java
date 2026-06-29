package com.example.rental.service.impl;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Transaction;
import com.example.rental.model.User;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.TransactionService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

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
    public List<Transaction> findAll(User currentUser) {
        if (currentUser == null || userService.isAdmin(currentUser)) {
            return findAll();
        }
        Set<Long> allowedMotelIds = userService.getAssignedMotelIds(currentUser);
        return transactionRepository.findAllByOrderByNgayTaoDesc().stream()
                .filter(t -> {
                    if (t.getHoaDon() == null || t.getHoaDon().getHopDong() == null
                            || t.getHoaDon().getHopDong().getPhongTro() == null
                            || t.getHoaDon().getHopDong().getPhongTro().getNhaTro() == null) {
                        return false;
                    }
                    return allowedMotelIds.contains(
                            t.getHoaDon().getHopDong().getPhongTro().getNhaTro().getId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByInvoiceId(Long hoaDonId) {
        return transactionRepository.findByHoaDonId(hoaDonId);
    }

    @Override
    public List<Transaction> search(String keyword, String maHoaDon, String tenKhachThue,
                                    PaymentStatus trangThai, PaymentMethod hinhThucTT,
                                    LocalDateTime tuNgay, LocalDateTime denNgay) {
        return search(keyword, maHoaDon, tenKhachThue, trangThai, hinhThucTT, tuNgay, denNgay, null);
    }

    @Override
    public List<Transaction> search(String keyword, String maHoaDon, String tenKhachThue,
                                    PaymentStatus trangThai, PaymentMethod hinhThucTT,
                                    LocalDateTime tuNgay, LocalDateTime denNgay, User currentUser) {
        return applyFilters(transactionRepository.findAllByOrderByNgayTaoDesc(), keyword, maHoaDon, tenKhachThue,
                trangThai, hinhThucTT, tuNgay, denNgay, currentUser);
    }

    @Override
    public List<Transaction> filterForExport(String keyword, String maHoaDon, String tenKhachThue,
                                             PaymentStatus trangThai, PaymentMethod hinhThucTT,
                                             LocalDateTime tuNgay, LocalDateTime denNgay) {
        return filterForExport(keyword, maHoaDon, tenKhachThue, trangThai, hinhThucTT, tuNgay, denNgay, null);
    }

    @Override
    public List<Transaction> filterForExport(String keyword, String maHoaDon, String tenKhachThue,
                                             PaymentStatus trangThai, PaymentMethod hinhThucTT,
                                             LocalDateTime tuNgay, LocalDateTime denNgay, User currentUser) {
        return applyFilters(transactionRepository.findAllByOrderByNgayTaoDesc(), keyword, maHoaDon, tenKhachThue,
                trangThai, hinhThucTT, tuNgay, denNgay, currentUser);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) throws Exception {
        Transaction transaction = findById(id);
        transactionRepository.delete(transaction);
    }

    private List<Transaction> applyFilters(List<Transaction> source, String keyword, String maHoaDon, String tenKhachThue,
                                            PaymentStatus trangThai, PaymentMethod hinhThucTT,
                                            LocalDateTime tuNgay, LocalDateTime denNgay, User currentUser) {
        List<Transaction> filtered = source;
        if (currentUser != null && !userService.isAdmin(currentUser)) {
            Set<Long> allowedMotelIds = userService.getAssignedMotelIds(currentUser);
            final Set<Long> finalAllowed = allowedMotelIds;
            filtered = source.stream()
                    .filter(t -> {
                        if (t.getHoaDon() == null || t.getHoaDon().getHopDong() == null
                                || t.getHoaDon().getHopDong().getPhongTro() == null
                                || t.getHoaDon().getHopDong().getPhongTro().getNhaTro() == null) {
                            return false;
                        }
                        return finalAllowed.contains(
                                t.getHoaDon().getHopDong().getPhongTro().getNhaTro().getId());
                    })
                    .collect(Collectors.toList());
        }
        String normalizedKeyword = normalize(keyword);
        String normalizedMaHoaDon = normalize(maHoaDon);
        String normalizedTenKhach = normalize(tenKhachThue);

        return filtered.stream()
                .filter(t -> normalizedKeyword == null || matchesKeyword(t, normalizedKeyword))
                .filter(t -> normalizedMaHoaDon == null || matchesMaHoaDon(t, normalizedMaHoaDon))
                .filter(t -> normalizedTenKhach == null || matchesTenKhachThue(t, normalizedTenKhach))
                .filter(t -> trangThai == null || t.getTrangThai() == trangThai)
                .filter(t -> hinhThucTT == null || t.getHinhThucTT() == hinhThucTT)
                .filter(t -> tuNgay == null || (t.getNgayTao() != null && !t.getNgayTao().isBefore(tuNgay)))
                .filter(t -> denNgay == null || (t.getNgayTao() != null && !t.getNgayTao().isAfter(denNgay)))
                .sorted(Comparator.comparing(Transaction::getNgayTao, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeyword(Transaction t, String keyword) {
        if (t.getMaGiaoDich() != null && t.getMaGiaoDich().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        if (t.getGhiChu() != null && t.getGhiChu().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        if (t.getMaCongTT() != null && t.getMaCongTT().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        if (t.getDuLieuTT() != null && t.getDuLieuTT().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        if (matchesMaHoaDon(t, keyword)) return true;
        return matchesTenKhachThue(t, keyword);
    }

    private boolean matchesMaHoaDon(Transaction t, String maHoaDon) {
        return t.getHoaDon() != null && t.getHoaDon().getMaHoaDon() != null
                && t.getHoaDon().getMaHoaDon().toLowerCase(Locale.ROOT).contains(maHoaDon);
    }

    private boolean matchesTenKhachThue(Transaction t, String tenKhach) {
        if (t.getHoaDon() == null || t.getHoaDon().getHopDong() == null) return false;
        if (t.getHoaDon().getHopDong().getKhachThue() == null) return false;
        String name = t.getHoaDon().getHopDong().getKhachThue().getHoTen();
        return name != null && name.toLowerCase(Locale.ROOT).contains(tenKhach);
    }
}
