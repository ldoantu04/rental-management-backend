package com.example.rental.service.utils;

import com.example.rental.domain.WaterCalculationType;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.InvoiceServiceItemRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralised pricing engine for invoices.
 *
 * <p>Single source of truth for:
 * <ul>
 *   <li>resolving the room / electricity / water unit price from the contract
 *       (with safe fallbacks to the previous invoice for legacy data),</li>
 *   <li>building {@link InvoiceServiceItem} rows (one per line: room, electric, water, services),</li>
 *   <li>computing the invoice total strictly as the sum of line totals.</li>
 * </ul>
 *
 * <p>Anything that ends up in {@code Invoice.tongTien} MUST be the result of
 * {@link #computeTotal(Invoice, InvoiceRequest, Contract)} -- never a manually
 * recomputed value in the controller or business tool.</p>
 */
public final class InvoicePricingEngine {

    private InvoicePricingEngine() {}

    // ====================================================================
    //  Resolve unit prices
    // ====================================================================

    public static BigDecimal resolveRoomPrice(Contract contract, Invoice previous) {
        if (contract != null && contract.getGiaThue() != null) {
            return contract.getGiaThue();
        }
        if (previous != null && previous.getTienPhong() != null) {
            return previous.getTienPhong();
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal resolveElectricPrice(Contract contract, Invoice previous, InvoiceRequest req) {
        if (req != null && req.getGiaDien() != null) return req.getGiaDien();
        if (contract != null && contract.getGiaDien() != null) return contract.getGiaDien();
        if (previous != null && previous.getGiaDien() != null) return previous.getGiaDien();
        return BigDecimal.ZERO;
    }

    public static BigDecimal resolveWaterPrice(Contract contract, Invoice previous, InvoiceRequest req) {
        if (req != null && req.getGiaNuoc() != null) return req.getGiaNuoc();
        if (contract != null && contract.getGiaNuoc() != null) return contract.getGiaNuoc();
        if (previous != null && previous.getGiaNuoc() != null) return previous.getGiaNuoc();
        return BigDecimal.ZERO;
    }

    public static WaterCalculationType resolveWaterCalc(Contract contract, Invoice previous, InvoiceRequest req) {
        if (req != null && req.getKieuTinhNuoc() != null) return req.getKieuTinhNuoc();
        if (contract != null && contract.getKieuTinhNuoc() != null) return contract.getKieuTinhNuoc();
        if (previous != null && previous.getKieuTinhNuoc() != null) return previous.getKieuTinhNuoc();
        return WaterCalculationType.CHI_SO;
    }

    // ====================================================================
    //  Build InvoiceServiceItem rows
    // ====================================================================

    public static List<InvoiceServiceItem> buildItems(Invoice invoice,
                                                      InvoiceRequest req,
                                                      Contract contract,
                                                      Invoice previous) {
        List<InvoiceServiceItem> result = new ArrayList<>();

        BigDecimal roomPrice = resolveRoomPrice(contract, previous);
        if (req != null && req.getTienPhong() != null) {
            roomPrice = req.getTienPhong();
        }
        if (roomPrice.signum() > 0) {
            result.add(lineItem(invoice, "Tien phong", "Theo phong", BigDecimal.ONE, roomPrice, true));
        }

        BigDecimal electricUnit = resolveElectricPrice(contract, previous, req);
        Integer dienCu = req != null ? req.getChiSoDienCu()
                : (previous != null ? previous.getChiSoDienMoi() : null);
        Integer dienMoi = req != null ? req.getChiSoDienMoi() : null;
        BigDecimal electricQty = safeDiff(dienMoi, dienCu);
        if (electricUnit.signum() > 0 && electricQty.signum() > 0) {
            result.add(lineItem(invoice, "Tien dien", "Theo chi so", electricQty, electricUnit, true));
        }

        BigDecimal waterUnit = resolveWaterPrice(contract, previous, req);
        WaterCalculationType waterType = resolveWaterCalc(contract, previous, req);
        BigDecimal waterQty = BigDecimal.ONE;
        switch (waterType) {
            case CHI_SO -> {
                Integer nuocCu = req != null ? req.getChiSoNuocCu()
                        : (previous != null ? previous.getChiSoNuocMoi() : null);
                Integer nuocMoi = req != null ? req.getChiSoNuocMoi() : null;
                waterQty = safeDiff(nuocMoi, nuocCu);
            }
            case THEO_NGUOI -> waterQty = BigDecimal.valueOf(countPeople(contract));
            case THEO_PHONG -> waterQty = BigDecimal.ONE;
        }
        if (waterUnit.signum() > 0 && waterQty.signum() > 0) {
            result.add(lineItem(invoice, "Tien nuoc", "Theo " + waterType.name().toLowerCase(), waterQty, waterUnit, true));
        }

        if (contract != null && contract.getDanhSachDichVu() != null) {
            for (var csi : contract.getDanhSachDichVu()) {
                if (csi == null || csi.getTenDichVu() == null || csi.getTenDichVu().isBlank()) continue;
                // Skip hard-coded utility lines that the engine already created
                // (room / electric / water). The contract's service list MUST
                // only contain additional services (internet, parking, ...).
                if (isUtilityLine(csi.getTenDichVu())) continue;
                BigDecimal qty = csi.getSoLuong() != null ? csi.getSoLuong() : BigDecimal.ONE;
                BigDecimal unit = csi.getDonGia() != null ? csi.getDonGia() : BigDecimal.ZERO;
                result.add(lineItem(invoice, csi.getTenDichVu(),
                        csi.getKieuTinh() != null ? csi.getKieuTinh() : "Theo phong",
                        qty, unit, true));
            }
        }

        if (req != null && req.getDanhSachDichVu() != null) {
            for (InvoiceServiceItemRequest extra : req.getDanhSachDichVu()) {
                if (extra == null || extra.getTenDichVu() == null || extra.getTenDichVu().isBlank()) continue;
                if (isUtilityLine(extra.getTenDichVu())) continue;
                if (Boolean.TRUE.equals(extra.getLaTuHopDong())) continue;
                BigDecimal qty = extra.getSoLuong() != null ? extra.getSoLuong() : BigDecimal.ONE;
                BigDecimal unit = extra.getDonGia() != null ? extra.getDonGia() : BigDecimal.ZERO;
                result.add(lineItem(invoice, extra.getTenDichVu(),
                        extra.getKieuTinh() != null ? extra.getKieuTinh() : "Theo phong",
                        qty, unit, extra.getLaTuHopDong() == null ? Boolean.FALSE : extra.getLaTuHopDong()));
            }
        }

        return result;
    }

    /**
     * Returns true if the given service name is actually a hard-coded utility
     * line (room rent, electricity, water) and therefore must NOT be added as
     * an extra {@link InvoiceServiceItem}. Comparison is case- and accent-
     * insensitive.
     */
    public static boolean isUtilityLine(String tenDichVu) {
        if (tenDichVu == null) return false;
        String n = normalize(tenDichVu);
        for (String tag : UTILITY_TAGS) {
            if (n.contains(tag)) return true;
        }
        return false;
    }

    private static final String[] UTILITY_TAGS = {
            "room", "rent", "tien phong", "phong",
            "electric", "electricity", "tien dien", "dien",
            "water", "tien nuoc", "nuoc"
    };

    private static String normalize(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(java.util.Locale.ROOT).trim();
    }

    // ====================================================================
    //  Total
    // ====================================================================

    public static BigDecimal computeTotal(Invoice invoice, InvoiceRequest req, Contract contract) {
        List<InvoiceServiceItem> items = invoice.getDanhSachDichVu();
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (InvoiceServiceItem it : items) {
                if (it.getThanhTien() != null) sum = sum.add(it.getThanhTien());
            }
        }
        if (sum.signum() > 0) return sum;
        // Backward-compat fallback: no items yet (caller hasn't persisted them).
        // Use the request values to compute without persisting.
        BigDecimal room = resolveRoomPrice(contract, null);
        if (req != null && req.getTienPhong() != null) room = req.getTienPhong();
        BigDecimal dien = computeElectric(req, contract);
        BigDecimal nuoc = computeWater(req, contract);
        BigDecimal dv = computeServices(req);
        return room.add(dien).add(nuoc).add(dv);
    }

    public static BigDecimal computeElectric(InvoiceRequest req, Contract contract) {
        if (req == null) return BigDecimal.ZERO;
        if (req.getChiSoDienMoi() == null || req.getChiSoDienCu() == null || req.getGiaDien() == null) {
            return BigDecimal.ZERO;
        }
        int diff = Math.max(req.getChiSoDienMoi() - req.getChiSoDienCu(), 0);
        return req.getGiaDien().multiply(BigDecimal.valueOf(diff));
    }

    public static BigDecimal computeWater(InvoiceRequest req, Contract contract) {
        if (req == null || req.getGiaNuoc() == null) return BigDecimal.ZERO;
        WaterCalculationType type = req.getKieuTinhNuoc() != null ? req.getKieuTinhNuoc()
                : (contract != null && contract.getKieuTinhNuoc() != null ? contract.getKieuTinhNuoc()
                : WaterCalculationType.CHI_SO);
        return switch (type) {
            case THEO_PHONG -> req.getGiaNuoc();
            case THEO_NGUOI -> req.getGiaNuoc().multiply(BigDecimal.valueOf(Math.max(countPeople(contract), 1)));
            case CHI_SO -> {
                if (req.getChiSoNuocMoi() == null || req.getChiSoNuocCu() == null) yield BigDecimal.ZERO;
                int diff = Math.max(req.getChiSoNuocMoi() - req.getChiSoNuocCu(), 0);
                yield req.getGiaNuoc().multiply(BigDecimal.valueOf(diff));
            }
        };
    }

    public static BigDecimal computeServices(InvoiceRequest req) {
        if (req == null || req.getDanhSachDichVu() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceServiceItemRequest item : req.getDanhSachDichVu()) {
            if (item.getDonGia() == null) continue;
            BigDecimal qty = item.getSoLuong() != null ? item.getSoLuong() : BigDecimal.ONE;
            total = total.add(item.getDonGia().multiply(qty));
        }
        return total;
    }

    // ====================================================================
    //  Helpers
    // ====================================================================

    private static InvoiceServiceItem lineItem(Invoice invoice, String name, String calc,
                                               BigDecimal qty, BigDecimal unit, Boolean fromContract) {
        BigDecimal amount = qty.multiply(unit).setScale(0, RoundingMode.HALF_UP);
        InvoiceServiceItem it = new InvoiceServiceItem();
        it.setHoaDon(invoice);
        it.setTenDichVu(name);
        it.setKieuTinh(calc);
        it.setSoLuong(qty);
        it.setDonGia(unit);
        it.setThanhTien(amount);
        it.setLaTuHopDong(fromContract);
        return it;
    }

    private static BigDecimal safeDiff(Integer moi, Integer cu) {
        if (moi == null || cu == null) return BigDecimal.ZERO;
        int diff = moi - cu;
        if (diff < 0) diff = 0;
        return BigDecimal.valueOf(diff);
    }

    private static int countPeople(Contract contract) {
        if (contract == null) return 1;
        Tenant t = contract.getKhachThue();
        if (t != null) {
            int extra = t.getDanhSachNguoiOCung() != null ? t.getDanhSachNguoiOCung().size() : 0;
            return 1 + extra;
        }
        Room room = contract.getPhongTro();
        if (room != null && room.getSoNguoi() != null && room.getSoNguoi() > 0) {
            return room.getSoNguoi();
        }
        return 1;
    }
}
