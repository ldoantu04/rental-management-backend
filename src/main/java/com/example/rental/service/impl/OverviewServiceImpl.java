package com.example.rental.service.impl;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.OverviewResponse;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Room;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OverviewServiceImpl implements OverviewService {

    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public OverviewResponse getOverview() {
        OverviewResponse response = new OverviewResponse();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();

        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal tongDoanhThu = allInvoices.stream()
                .filter(inv -> inv.getTrangThai() == InvoiceStatus.DA_THANH_TOAN)
                .filter(inv -> inv.getKyHoaDon() != null
                        && inv.getKyHoaDon().getMonth() == today.getMonth()
                        && inv.getKyHoaDon().getYear() == today.getYear())
                .map(inv -> inv.getTongTien() != null ? inv.getTongTien() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTongDoanhThu(tongDoanhThu);

        List<Room> allRooms = roomRepository.findAll();
        int tongPhong = allRooms.size();
        int phongDangThue = (int) allRooms.stream()
                .filter(r -> r.getTrangThai() == RoomStatus.DANG_THUE)
                .count();
        response.setTongPhong(tongPhong);
        response.setPhongDangThue(phongDangThue);

        List<Invoice> hoaDonChuaThanhToan = invoiceRepository.findByTrangThai(InvoiceStatus.CHUA_THANH_TOAN);
        List<Invoice> hoaDonQuaHan = invoiceRepository.findByTrangThai(InvoiceStatus.QUA_HAN);
        response.setHoaDonChuaThanhToan(hoaDonChuaThanhToan.size() + hoaDonQuaHan.size());

        List<Contract> allContracts = contractRepository.findAll();
        List<Contract> hopDongSapHet = allContracts.stream()
                .filter(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC)
                .filter(c -> c.getNgayKetThuc() != null)
                .filter(c -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, c.getNgayKetThuc());
                    return daysUntilExpiry >= 0 && daysUntilExpiry <= 30;
                })
                .collect(Collectors.toList());
        response.setHopDongSapHetHan(hopDongSapHet.size());

        List<OverviewResponse.HopDongSapHet> hopDongSapHetList = new ArrayList<>();
        for (Contract c : hopDongSapHet) {
            OverviewResponse.HopDongSapHet item = new OverviewResponse.HopDongSapHet();
            item.setTenKhach(c.getKhachThue() != null ? c.getKhachThue().getHoTen() : "N/A");
            item.setTenTro(c.getPhongTro() != null && c.getPhongTro().getNhaTro() != null
                    ? c.getPhongTro().getNhaTro().getTenTro() : "N/A");
            item.setMaPhong(c.getPhongTro() != null ? c.getPhongTro().getMaPhong() : "N/A");
            item.setNgayHetHan(c.getNgayKetThuc().format(formatter));
            hopDongSapHetList.add(item);
        }
        response.setHopDongSapHet(hopDongSapHetList);

        List<OverviewResponse.KhachTreHan> khachTreHanList = new ArrayList<>();
        List<Invoice> invoiceQuaHan = invoiceRepository.findByTrangThai(InvoiceStatus.QUA_HAN);
        for (Invoice inv : hoaDonChuaThanhToan) {
            if (inv.getHanThanhToan() != null && inv.getHanThanhToan().isBefore(today)) {
                invoiceQuaHan.add(inv);
            }
        }

        for (Invoice inv : invoiceQuaHan) {
            OverviewResponse.KhachTreHan item = new OverviewResponse.KhachTreHan();
            if (inv.getHopDong() != null && inv.getHopDong().getKhachThue() != null) {
                item.setTenKhach(inv.getHopDong().getKhachThue().getHoTen());
            } else {
                item.setTenKhach("N/A");
            }
            if (inv.getHopDong() != null && inv.getHopDong().getPhongTro() != null) {
                item.setMaPhong(inv.getHopDong().getPhongTro().getMaPhong());
                if (inv.getHopDong().getPhongTro().getNhaTro() != null) {
                    item.setTenTro(inv.getHopDong().getPhongTro().getNhaTro().getTenTro());
                } else {
                    item.setTenTro("N/A");
                }
            } else {
                item.setMaPhong("N/A");
                item.setTenTro("N/A");
            }
            item.setSoTien(inv.getTongTien() != null ? inv.getTongTien() : BigDecimal.ZERO);
            if (inv.getHanThanhToan() != null) {
                item.setSoNgayTre(ChronoUnit.DAYS.between(inv.getHanThanhToan(), today));
            } else {
                item.setSoNgayTre(0);
            }
            khachTreHanList.add(item);
        }
        response.setKhachTreHanThanhToan(khachTreHanList);

        return response;
    }
}
