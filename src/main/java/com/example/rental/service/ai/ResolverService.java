package com.example.rental.service.ai;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.RoomStatus;
import com.example.rental.domain.TenantStatus;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Resolves business identifiers (room code, tenant name, contract code, ...) into
 * internal database IDs. All Business Tools MUST use this resolver so the LLM never
 * needs to know or pass an ID.
 */
@Service
@RequiredArgsConstructor
public class ResolverService {

    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final MotelRepository motelRepository;

    public Room resolveRoom(String roomCode, String motelName) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Thieu ma phong");
        }
        String code = roomCode.trim();
        List<Room> all = roomRepository.findAll();
        List<Room> matches = all.stream()
                .filter(r -> r.getMaPhong() != null && r.getMaPhong().equalsIgnoreCase(code))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay phong co ma '" + roomCode + "'");
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (motelName != null && !motelName.isBlank()) {
            String motelNeedle = motelName.trim().toLowerCase(Locale.ROOT);
            List<Room> filtered = matches.stream()
                    .filter(r -> r.getNhaTro() != null
                            && r.getNhaTro().getTenTro() != null
                            && r.getNhaTro().getTenTro().toLowerCase(Locale.ROOT).contains(motelNeedle))
                    .toList();
            if (filtered.size() == 1) {
                return filtered.get(0);
            }
            if (filtered.isEmpty()) {
                throw new IllegalArgumentException(
                        "Khong tim thay phong '" + roomCode + "' thuoc nha tro '" + motelName + "'");
            }
        }
        StringBuilder sb = new StringBuilder("Co ").append(matches.size())
                .append(" phong trung ma '").append(roomCode).append("'. Vui long chi ro nha tro:");
        for (Room r : matches) {
            sb.append("\n- ").append(r.getMaPhong())
                    .append(" (nha tro: ").append(r.getNhaTro() != null ? r.getNhaTro().getTenTro() : "N/A")
                    .append(")");
        }
        throw new AmbiguousMatchException(sb.toString());
    }

    public Tenant resolveTenant(String hoTen, String sdt, String cccd) {
        List<Tenant> all = tenantRepository.findAll();
        if (cccd != null && !cccd.isBlank()) {
            for (Tenant t : all) {
                if (t.getCccd() != null && t.getCccd().equalsIgnoreCase(cccd.trim())) {
                    return t;
                }
            }
        }
        if (sdt != null && !sdt.isBlank()) {
            String phone = sdt.trim();
            for (Tenant t : all) {
                if (t.getSdt() != null && t.getSdt().contains(phone)) {
                    return t;
                }
            }
        }
        if (hoTen == null || hoTen.isBlank()) {
            throw new IllegalArgumentException("Can cung cap ho ten khach thue");
        }
        String needle = normalize(hoTen);
        List<Tenant> matches = all.stream()
                .filter(t -> t.getHoTen() != null && normalize(t.getHoTen()).contains(needle))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay khach thue ten '" + hoTen + "'");
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        StringBuilder sb = new StringBuilder("Co ").append(matches.size())
                .append(" khach trung ten '").append(hoTen).append("'. Vui long chi ro SDT hoac CCCD:");
        for (Tenant t : matches) {
            sb.append("\n- ").append(t.getHoTen())
                    .append(" (SDT: ").append(t.getSdt() == null ? "N/A" : t.getSdt())
                    .append(", CCCD: ").append(t.getCccd() == null ? "N/A" : t.getCccd())
                    .append(")");
        }
        throw new AmbiguousMatchException(sb.toString());
    }

    public Motel resolveMotel(String motelName) {
        if (motelName == null || motelName.isBlank()) {
            List<Motel> all = motelRepository.findAll();
            if (all.size() == 1) return all.get(0);
            if (all.isEmpty()) throw new IllegalArgumentException("He thong chua co nha tro nao");
            throw new AmbiguousMatchException("Co " + all.size() + " nha tro, vui long chi ro ten nha tro");
        }
        String needle = normalize(motelName);
        List<Motel> matches = motelRepository.findAll().stream()
                .filter(m -> m.getTenTro() != null && normalize(m.getTenTro()).contains(needle))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay nha tro ten '" + motelName + "'");
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        StringBuilder sb = new StringBuilder("Co ").append(matches.size())
                .append(" nha tro trung ten '").append(motelName).append("':");
        for (Motel m : matches) {
            sb.append("\n- ").append(m.getTenTro()).append(" (").append(m.getDiaChi()).append(")");
        }
        throw new AmbiguousMatchException(sb.toString());
    }

    /**
     * Resolve the contract that should be used for invoice creation, etc.
     * Logic (per product rules):
     *   1. Prefer the most recent contract that is currently {@link ContractStatus#DANG_HIEU_LUC}.
     *   2. If none, but the room is still {@link RoomStatus#DANG_THUE}, accept the most
     *      recent non-cancelled contract (expiry alone does not invalidate it).
     *   3. Otherwise, throw.
     */
    public Contract resolveCurrentContract(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Thieu phong");
        }
        List<Contract> all = contractRepository.findByPhongTroIdAndTrangThaiNot(
                room.getId(), ContractStatus.DA_HUY);

        if (all == null || all.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phong " + room.getMaPhong() + " chua tung co hop dong nao");
        }

        // Step 1: prefer the freshest ACTIVE contract
        List<Contract> active = all.stream()
                .filter(c -> c.getTrangThai() == ContractStatus.DANG_HIEU_LUC)
                .sorted(Comparator.comparing(
                        Contract::getNgayBatDau, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!active.isEmpty()) {
            return active.get(active.size() - 1);
        }

        // Step 2: room still DANG_THUE -> fall back to the most recent non-cancelled contract
        if (room.getTrangThai() == RoomStatus.DANG_THUE) {
            List<Contract> candidates = all.stream()
                    .filter(c -> c.getTrangThai() != ContractStatus.DA_HUY)
                    .sorted(Comparator.comparing(
                            Contract::getNgayBatDau, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (!candidates.isEmpty()) {
                return candidates.get(candidates.size() - 1);
            }
        }
        throw new IllegalArgumentException(
                "Phong " + room.getMaPhong() + " khong co hop dong nao de su dung");
    }

    /**
     * Strict version that requires {@code DANG_HIEU_LUC}. Kept for read-only checks
     * (e.g. when the caller explicitly wants the contract in force today).
     */
    public Contract resolveActiveContract(Room room) throws Exception {
        List<Contract> active = contractRepository.findByPhongTroIdAndTrangThai(
                room.getId(), ContractStatus.DANG_HIEU_LUC);
        if (active == null || active.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phong " + room.getMaPhong() + " khong co hop dong hieu luc");
        }
        if (active.size() == 1) {
            return active.get(0);
        }
        active.sort(Comparator.comparing(Contract::getNgayBatDau, Comparator.nullsLast(Comparator.naturalOrder())));
        return active.get(active.size() - 1);
    }

    public Contract resolveContractByCode(String maHopDong) throws Exception {
        if (maHopDong == null || maHopDong.isBlank()) {
            throw new IllegalArgumentException("Thieu ma hop dong");
        }
        for (Contract c : contractRepository.findAll()) {
            if (c.getMaHopDong() != null && c.getMaHopDong().equalsIgnoreCase(maHopDong.trim())) {
                return c;
            }
        }
        throw new IllegalArgumentException("Khong tim thay hop dong ma '" + maHopDong + "'");
    }

    /**
     * Returns the full history of contracts attached to a room, newest first.
     */
    public List<Contract> resolveContractsOfRoom(Room room) {
        if (room == null) return List.of();
        return contractRepository.findByPhongTroIdAndTrangThaiNot(
                        room.getId(), ContractStatus.DA_HUY).stream()
                .sorted(Comparator.comparing(
                        Contract::getNgayBatDau, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Returns the room currently occupied by a tenant (the most recent
     * non-cancelled contract that still points to a room).
     */
    public Room resolveRoomByTenant(Tenant tenant) {
        if (tenant == null) return null;
        List<Contract> all = contractRepository.findAll().stream()
                .filter(c -> c.getKhachThue() != null
                        && c.getKhachThue().getId().equals(tenant.getId()))
                .filter(c -> c.getTrangThai() != ContractStatus.DA_HUY)
                .filter(c -> c.getPhongTro() != null)
                .sorted(Comparator.comparing(
                        Contract::getNgayBatDau, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (all.isEmpty()) return null;
        return all.get(all.size() - 1).getPhongTro();
    }

    /**
     * Tries to find an invoice by its code. Falls back to fuzzy match (contains)
     * if an exact match is not found, so the LLM (or user) can pass abbreviated codes.
     */
    public Invoice resolveInvoiceByCodeFlexible(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.isBlank()) return null;
        String needle = maHoaDon.trim();
        Invoice exact = null;
        Invoice fuzzy = null;
        for (Invoice i : invoiceRepository.findAll()) {
            if (i.getMaHoaDon() == null) continue;
            if (i.getMaHoaDon().equalsIgnoreCase(needle)) {
                exact = i;
                break;
            }
            if (fuzzy == null && i.getMaHoaDon().toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT))) {
                fuzzy = i;
            }
        }
        return exact != null ? exact : fuzzy;
    }

    public Invoice resolveInvoice(String maHoaDon) throws Exception {
        if (maHoaDon == null || maHoaDon.isBlank()) {
            throw new IllegalArgumentException("Thieu ma hoa don");
        }
        for (Invoice i : invoiceRepository.findAll()) {
            if (i.getMaHoaDon() != null && i.getMaHoaDon().equalsIgnoreCase(maHoaDon.trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Khong tim thay hoa don ma '" + maHoaDon + "'");
    }

    public Invoice findLatestInvoice(Contract contract) {
        Invoice latest = null;
        for (Invoice i : invoiceRepository.findAll()) {
            if (i.getHopDong() == null || !i.getHopDong().getId().equals(contract.getId())) continue;
            if (latest == null) { latest = i; continue; }
            if (i.getKyHoaDon() == null) continue;
            if (latest.getKyHoaDon() == null || i.getKyHoaDon().isAfter(latest.getKyHoaDon())) {
                latest = i;
            }
        }
        return latest;
    }

    public RoomStatus inferRoomStatusForNewContract(Room room) {
        if (room.getTrangThai() == RoomStatus.BAO_TRI) {
            throw new IllegalArgumentException(
                    "Phong " + room.getMaPhong() + " dang bao tri, khong the tao hop dong");
        }
        return room.getTrangThai();
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    public static class AmbiguousMatchException extends RuntimeException {
        public AmbiguousMatchException(String msg) { super(msg); }
    }
}
