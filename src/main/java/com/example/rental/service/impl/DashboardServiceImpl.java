package com.example.rental.service.impl;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.DashboardFilterResponse;
import com.example.rental.dto.OverviewResponse;
import com.example.rental.dto.RevenueChartResponse;
import com.example.rental.dto.RoomStatusResponse;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.Transaction;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.DashboardService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;
    private final RoomRepository roomRepository;
    private final MotelRepository motelRepository;
    private final UserService userService;
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;

    @Override
    public OverviewResponse getOverview() {
        OverviewResponse response = new OverviewResponse();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();

        // Tổng doanh thu tháng: tính từ Transaction thành công trong tháng hiện tại
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);
        BigDecimal tongDoanhThu = transactionRepository.findAll().stream()
                .filter(tx -> tx.getTrangThai() == com.example.rental.domain.PaymentStatus.THANH_CONG)
                .filter(tx -> tx.getNgayThanhToan() != null)
                .filter(tx -> !tx.getNgayThanhToan().isBefore(startOfMonth))
                .filter(tx -> !tx.getNgayThanhToan().isAfter(endOfMonth))
                .map(tx -> tx.getSoTien() != null ? tx.getSoTien() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTongDoanhThu(tongDoanhThu);

        List<Room> allRooms = roomRepository.findAll();
        int tongPhong = allRooms.size();
        int phongDangThue = (int) allRooms.stream()
                .filter(r -> r.getTrangThai() == RoomStatus.DANG_THUE)
                .count();
        response.setTongPhong(tongPhong);
        response.setPhongDangThue(phongDangThue);

        List<com.example.rental.model.Invoice> hoaDonChuaThanhToan = invoiceRepository.findByTrangThai(InvoiceStatus.CHUA_THANH_TOAN);
        List<com.example.rental.model.Invoice> hoaDonQuaHan = invoiceRepository.findByTrangThai(InvoiceStatus.QUA_HAN);
        response.setHoaDonChuaThanhToan(hoaDonChuaThanhToan.size() + hoaDonQuaHan.size());

        List<com.example.rental.model.Contract> allContracts = contractRepository.findAll();
        List<com.example.rental.model.Contract> hopDongSapHet = allContracts.stream()
                .filter(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC)
                .filter(c -> c.getNgayKetThuc() != null)
                .filter(c -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, c.getNgayKetThuc());
                    return daysUntilExpiry >= 0 && daysUntilExpiry <= 30;
                })
                .collect(Collectors.toList());
        response.setHopDongSapHetHan(hopDongSapHet.size());

        List<OverviewResponse.HopDongSapHet> hopDongSapHetList = new ArrayList<>();
        for (com.example.rental.model.Contract c : hopDongSapHet) {
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
        List<com.example.rental.model.Invoice> invoiceQuaHan = new ArrayList<>(hoaDonQuaHan);
        for (com.example.rental.model.Invoice inv : hoaDonChuaThanhToan) {
            if (inv.getHanThanhToan() != null && inv.getHanThanhToan().isBefore(today)) {
                invoiceQuaHan.add(inv);
            }
        }

        for (com.example.rental.model.Invoice inv : invoiceQuaHan) {
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

    @Override
    public List<RevenueChartResponse> getRevenueChart(int year, Long motelId, com.example.rental.model.User currentUser) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<Transaction> allTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getTrangThai() == PaymentStatus.THANH_CONG)
                .filter(t -> t.getNgayThanhToan() != null)
                .filter(t -> !t.getNgayThanhToan().isBefore(startOfYear))
                .filter(t -> !t.getNgayThanhToan().isAfter(endOfYear))
                .collect(Collectors.toList());

        if (motelId != null) {
            Set<Long> allowedMotelIds = getAllowedMotelIds(currentUser);
            if (!allowedMotelIds.isEmpty() && !allowedMotelIds.contains(motelId)) {
                return createEmptyYearData(year);
            }
            allTransactions = allTransactions.stream()
                    .filter(t -> t.getHoaDon() != null
                            && t.getHoaDon().getHopDong() != null
                            && t.getHoaDon().getHopDong().getPhongTro() != null
                            && t.getHoaDon().getHopDong().getPhongTro().getNhaTro() != null
                            && motelId.equals(t.getHoaDon().getHopDong().getPhongTro().getNhaTro().getId()))
                    .collect(Collectors.toList());
        } else if (currentUser != null && !userService.isAdmin(currentUser)) {
            Set<Long> allowedMotelIds = getAllowedMotelIds(currentUser);
            if (!allowedMotelIds.isEmpty()) {
                allTransactions = allTransactions.stream()
                        .filter(t -> t.getHoaDon() != null
                                && t.getHoaDon().getHopDong() != null
                                && t.getHoaDon().getHopDong().getPhongTro() != null
                                && t.getHoaDon().getHopDong().getPhongTro().getNhaTro() != null
                                && allowedMotelIds.contains(t.getHoaDon().getHopDong().getPhongTro().getNhaTro().getId()))
                        .collect(Collectors.toList());
            }
        }

        Map<Integer, BigDecimal> monthlyRevenue = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyRevenue.put(i, BigDecimal.ZERO);
        }

        for (Transaction tx : allTransactions) {
            int month = tx.getNgayThanhToan().getMonthValue();
            BigDecimal amount = tx.getSoTien() != null ? tx.getSoTien() : BigDecimal.ZERO;
            monthlyRevenue.merge(month, amount, BigDecimal::add);
        }

        List<RevenueChartResponse> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            RevenueChartResponse dto = new RevenueChartResponse();
            dto.setMonth(month);
            dto.setRevenue(monthlyRevenue.getOrDefault(month, BigDecimal.ZERO));
            result.add(dto);
        }
        return result;
    }

    @Override
    public RoomStatusResponse getRoomStatus(Long motelId, com.example.rental.model.User currentUser) {
        List<Room> rooms;
        if (motelId != null) {
            rooms = roomRepository.findByNhaTroId(motelId);
        } else {
            rooms = roomRepository.findAll();
            if (currentUser != null && !userService.isAdmin(currentUser)) {
                Set<Long> allowedMotelIds = getAllowedMotelIds(currentUser);
                if (!allowedMotelIds.isEmpty()) {
                    rooms = rooms.stream()
                            .filter(r -> r.getNhaTro() != null && allowedMotelIds.contains(r.getNhaTro().getId()))
                            .collect(Collectors.toList());
                }
            }
        }

        RoomStatusResponse dto = new RoomStatusResponse();
        dto.setRented(0);
        dto.setVacant(0);
        dto.setMaintenance(0);
        dto.setTotal(rooms.size());

        for (Room room : rooms) {
            if (room.getTrangThai() == null) continue;
            switch (room.getTrangThai()) {
                case DANG_THUE -> dto.setRented(dto.getRented() + 1);
                case TRONG -> dto.setVacant(dto.getVacant() + 1);
                case BAO_TRI -> dto.setMaintenance(dto.getMaintenance() + 1);
            }
        }
        return dto;
    }

    @Override
    public DashboardFilterResponse getFilters(com.example.rental.model.User currentUser) {
        DashboardFilterResponse dto = new DashboardFilterResponse();

        Set<Integer> yearSet = new TreeSet<>();
        List<Transaction> allTx = transactionRepository.findAll().stream()
                .filter(t -> t.getTrangThai() == PaymentStatus.THANH_CONG)
                .filter(t -> t.getNgayThanhToan() != null)
                .collect(Collectors.toList());

        if (currentUser != null && !userService.isAdmin(currentUser)) {
            Set<Long> allowedMotelIds = getAllowedMotelIds(currentUser);
            if (!allowedMotelIds.isEmpty()) {
                final Set<Long> finalAllowed = allowedMotelIds;
                allTx = allTx.stream()
                        .filter(t -> t.getHoaDon() != null
                                && t.getHoaDon().getHopDong() != null
                                && t.getHoaDon().getHopDong().getPhongTro() != null
                                && t.getHoaDon().getHopDong().getPhongTro().getNhaTro() != null
                                && finalAllowed.contains(t.getHoaDon().getHopDong().getPhongTro().getNhaTro().getId()))
                        .collect(Collectors.toList());
            }
        }

        for (Transaction tx : allTx) {
            yearSet.add(tx.getNgayThanhToan().getYear());
        }
        if (yearSet.isEmpty()) {
            yearSet.add(java.time.LocalDate.now().getYear());
        }

        List<Integer> years = new ArrayList<>(yearSet);
        dto.setYears(years);

        List<Motel> motels = motelRepository.findAll();
        if (currentUser != null && !userService.isAdmin(currentUser)) {
            Set<Long> allowedMotelIds = getAllowedMotelIds(currentUser);
            if (!allowedMotelIds.isEmpty()) {
                motels = motels.stream()
                        .filter(m -> allowedMotelIds.contains(m.getId()))
                        .collect(Collectors.toList());
            }
        }

        List<DashboardFilterResponse.MotelOption> motelOptions = motels.stream()
                .map(m -> new DashboardFilterResponse.MotelOption(m.getId(), m.getTenTro()))
                .collect(Collectors.toList());
        dto.setMotels(motelOptions);

        return dto;
    }

    private Set<Long> getAllowedMotelIds(com.example.rental.model.User currentUser) {
        if (currentUser == null) return Collections.emptySet();
        return userService.getAssignedMotelIds(currentUser);
    }

    private List<RevenueChartResponse> createEmptyYearData(int year) {
        List<RevenueChartResponse> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            RevenueChartResponse dto = new RevenueChartResponse();
            dto.setMonth(month);
            dto.setRevenue(BigDecimal.ZERO);
            result.add(dto);
        }
        return result;
    }
}
