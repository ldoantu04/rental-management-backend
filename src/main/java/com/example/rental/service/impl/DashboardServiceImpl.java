package com.example.rental.service.impl;

import com.example.rental.domain.PaymentStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.DashboardFilterDTO;
import com.example.rental.dto.RevenueChartDTO;
import com.example.rental.dto.RoomStatusDTO;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.Transaction;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.service.DashboardService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;
    private final RoomRepository roomRepository;
    private final MotelRepository motelRepository;
    private final UserService userService;

    @Override
    public List<RevenueChartDTO> getRevenueChart(int year, Long motelId, com.example.rental.model.User currentUser) {
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

        List<RevenueChartDTO> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            RevenueChartDTO dto = new RevenueChartDTO();
            dto.setMonth(month);
            dto.setRevenue(monthlyRevenue.getOrDefault(month, BigDecimal.ZERO));
            result.add(dto);
        }
        return result;
    }

    @Override
    public RoomStatusDTO getRoomStatus(Long motelId, com.example.rental.model.User currentUser) {
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

        RoomStatusDTO dto = new RoomStatusDTO();
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
    public DashboardFilterDTO getFilters(com.example.rental.model.User currentUser) {
        DashboardFilterDTO dto = new DashboardFilterDTO();

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

        List<DashboardFilterDTO.MotelOption> motelOptions = motels.stream()
                .map(m -> new DashboardFilterDTO.MotelOption(m.getId(), m.getTenTro()))
                .collect(Collectors.toList());
        dto.setMotels(motelOptions);

        return dto;
    }

    private Set<Long> getAllowedMotelIds(com.example.rental.model.User currentUser) {
        if (currentUser == null) return Collections.emptySet();
        return userService.getAssignedMotelIds(currentUser);
    }

    private List<RevenueChartDTO> createEmptyYearData(int year) {
        List<RevenueChartDTO> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            RevenueChartDTO dto = new RevenueChartDTO();
            dto.setMonth(month);
            dto.setRevenue(BigDecimal.ZERO);
            result.add(dto);
        }
        return result;
    }
}
