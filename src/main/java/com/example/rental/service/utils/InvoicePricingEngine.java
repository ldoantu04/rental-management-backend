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
 * Single source of truth for building {@link InvoiceServiceItem} rows.
 *
 * <p>Design principles:</p>
 * <ul>
 *   <li>Contract = only stores DEFAULT unit prices for each service category.</li>
 *   <li>Invoice = the only place where actual billing data lives
 *       (actual qty, actual unit price, actual total, meter readings).</li>
 *   <li>Electricity &amp; water are NOT regular services — they are always created
 *       by this engine from meter readings and the contract/request prices.
 *       They carry their own meter readings (chiSoDau, chiSoCuoi).</li>
 *   <li>Only services marked {@code laDichVuBoSung = true} in the contract
 *       are copied to the invoice as regular service lines.</li>
 *   <li>Each line item carries a {@code loaiDichVu} tag so rendering code
 *       never needs to compare service names.</li>
 * </ul>
 *
 * <p>Service type tags:</p>
 * <ul>
 *   <li>PHONG — room rental</li>
 *   <li>DIEN — electricity (with meter readings)</li>
 *   <li>NUOC — water (with meter readings when CHI_SO)</li>
 *   <li>DICH_VU — additional services from contract or user-added</li>
 * </ul>
 */
public final class InvoicePricingEngine {

    public static final String LOAI_PHONG = "PHONG";
    public static final String LOAI_DIEN = "DIEN";
    public static final String LOAI_NUOC = "NUOC";
    public static final String LOAI_DICH_VU = "DICH_VU";

    private InvoicePricingEngine() {}

    // ====================================================================
    //  Resolve default prices (from contract or previous invoice)
    // ====================================================================

    /** Room price: contract.giaThue, fallback to previous invoice. */
    public static BigDecimal resolveRoomPrice(Contract contract, Invoice previous) {
        if (contract != null && contract.getGiaThue() != null) {
            return contract.getGiaThue();
        }
        if (previous != null && previous.getTienPhong() != null) {
            return previous.getTienPhong();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Electric price: request override &gt; contract &gt; previous invoice.
     * Returns ZERO if no price is available.
     */
    public static BigDecimal resolveElectricPrice(Contract contract, Invoice previous, InvoiceRequest req) {
        if (req != null && req.getGiaDien() != null) return req.getGiaDien();
        if (contract != null && contract.getGiaDien() != null) return contract.getGiaDien();
        if (previous != null && previous.getGiaDien() != null) return previous.getGiaDien();
        return BigDecimal.ZERO;
    }

    /**
     * Water price: request override &gt; contract &gt; previous invoice.
     * Returns ZERO if no price is available.
     */
    public static BigDecimal resolveWaterPrice(Contract contract, Invoice previous, InvoiceRequest req) {
        if (req != null && req.getGiaNuoc() != null) return req.getGiaNuoc();
        if (contract != null && contract.getGiaNuoc() != null) return contract.getGiaNuoc();
        if (previous != null && previous.getGiaNuoc() != null) return previous.getGiaNuoc();
        return BigDecimal.ZERO;
    }

    /**
     * Water calculation type: request override &gt; contract &gt; previous invoice.
     */
    public static WaterCalculationType resolveWaterCalc(Contract contract, Invoice previous, InvoiceRequest req) {
        if (req != null && req.getKieuTinhNuoc() != null) return req.getKieuTinhNuoc();
        if (contract != null && contract.getKieuTinhNuoc() != null) return contract.getKieuTinhNuoc();
        if (previous != null && previous.getKieuTinhNuoc() != null) return previous.getKieuTinhNuoc();
        return WaterCalculationType.CHI_SO;
    }

    // ====================================================================
    //  Build InvoiceServiceItem rows
    // ====================================================================

    /**
     * Builds the complete list of {@link InvoiceServiceItem} for an invoice.
     *
     * <ol>
     *   <li>Tiền phòng — always exactly one line (LOAI_PHONG).</li>
     *   <li>Tiền điện — always exactly one line, from meter readings (LOAI_DIEN).
     *       Never from contract services.</li>
     *   <li>Tiền nước — always exactly one line, from meter readings or
     *       per-person/per-room formula (LOAI_NUOC). Never from contract services.</li>
     *   <li>Additional services from contract — only those NOT matching utility names,
     *       with LOAI_DICH_VU.</li>
     *   <li>Extra services from request (user-added), with LOAI_DICH_VU.</li>
     * </ol>
     */
    public static List<InvoiceServiceItem> buildItems(Invoice invoice,
                                                      InvoiceRequest req,
                                                      Contract contract,
                                                      Invoice previous) {
        List<InvoiceServiceItem> result = new ArrayList<>();

        // ── 1. Tiền phòng ────────────────────────────────────────────────
        BigDecimal roomPrice = resolveRoomPrice(contract, previous);
        if (req != null && req.getTienPhong() != null) {
            roomPrice = req.getTienPhong();
        }
        if (roomPrice.signum() > 0) {
            result.add(lineItem(invoice, "Tiền phòng", "Phòng",
                    BigDecimal.ONE, roomPrice, true, LOAI_PHONG, null, null));
        }

        // ── 2. Tiền điện ────────────────────────────────────────────────
        BigDecimal electricUnit = resolveElectricPrice(contract, previous, req);
        Integer electricStart = resolveElectricStart(req, previous);
        Integer electricEnd = req != null ? req.getChiSoDienMoi() : null;
        BigDecimal electricQty = safeDiff(electricEnd, electricStart);
        if (electricUnit.signum() > 0 && electricQty.signum() > 0) {
            result.add(lineItem(invoice, "Tiền điện", "Số",
                    electricQty, electricUnit, true, LOAI_DIEN, electricStart, electricEnd));
        }

        // ── 3. Tiền nước ────────────────────────────────────────────────
        BigDecimal waterUnit = resolveWaterPrice(contract, previous, req);
        WaterCalculationType waterType = resolveWaterCalc(contract, previous, req);
        BigDecimal waterQty = resolveWaterQuantity(req, previous, waterType, contract);

        Integer waterStart = null;
        Integer waterEnd = null;
        if (waterType == WaterCalculationType.CHI_SO) {
            waterStart = resolveWaterStart(req, previous);
            waterEnd = req != null ? req.getChiSoNuocMoi() : null;
        }

        if (waterUnit.signum() > 0 && waterQty.signum() > 0) {
            String waterUnitLabel = switch (waterType) {
                case CHI_SO -> "m³";
                case THEO_NGUOI -> "người";
                case THEO_PHONG -> "phòng";
            };
            result.add(lineItem(invoice, "Tiền nước", waterUnitLabel,
                    waterQty, waterUnit, true, LOAI_NUOC, waterStart, waterEnd));
        }

        // ── 4. Additional services from contract ─────────────────────────
        // Skip services whose kieuTinh contains "chỉ số" — those are utility services
        // (electric/water by meter) handled by the dedicated Tiền điện / Tiền nước steps.
        if (contract != null && contract.getDanhSachDichVu() != null) {
            for (var csi : contract.getDanhSachDichVu()) {
                if (csi == null || csi.getTenDichVu() == null || csi.getTenDichVu().isBlank()) {
                    continue;
                }
                String kieuTinh = (csi.getKieuTinh() != null ? csi.getKieuTinh() : "").toLowerCase();
                if (kieuTinh.contains("chỉ số") || kieuTinh.contains("chi so")) {
                    continue;
                }
                if (isUtilityServiceName(csi.getTenDichVu())) {
                    continue;
                }
                BigDecimal unit = csi.getDonGia() != null ? csi.getDonGia() : BigDecimal.ZERO;
                String kieuTinhLabel = csi.getKieuTinh() != null ? csi.getKieuTinh() : "Theo phòng";
                BigDecimal qty = resolveContractServiceQuantity(kieuTinhLabel, contract);
                result.add(lineItem(invoice, csi.getTenDichVu(), kieuTinhLabel, qty, unit, true, LOAI_DICH_VU, null, null));
            }
        }

        // ── 5. Extra services from request (user-added only, laTuHopDong = false) ─
        // Skip laTuHopDong = true items — those came from contract and are already
        // added in step 4. Also skip items whose name is a utility service.
        if (req != null && req.getDanhSachDichVu() != null) {
            for (InvoiceServiceItemRequest extra : req.getDanhSachDichVu()) {
                if (extra == null || extra.getTenDichVu() == null || extra.getTenDichVu().isBlank()) {
                    continue;
                }
                if (Boolean.TRUE.equals(extra.getLaTuHopDong())) {
                    continue;
                }
                if (isUtilityServiceName(extra.getTenDichVu())) {
                    continue;
                }
                BigDecimal qty = extra.getSoLuong() != null ? extra.getSoLuong() : BigDecimal.ONE;
                BigDecimal unit = extra.getDonGia() != null ? extra.getDonGia() : BigDecimal.ZERO;
                BigDecimal amount = extra.getThanhTien();
                if (amount == null) {
                    amount = qty.multiply(unit).setScale(0, RoundingMode.HALF_UP);
                }
                String kieuTinh = extra.getKieuTinh() != null ? extra.getKieuTinh() : "Theo phòng";
                result.add(lineItem(invoice, extra.getTenDichVu(),
                        kieuTinh, qty, unit, amount, false, LOAI_DICH_VU, null, null));
            }
        }

        return result;
    }

    // ====================================================================
    //  Total
    // ====================================================================

    /**
     * Invoice total = sum of all {@link InvoiceServiceItem#thanhTien} + phiPhat.
     * This is the ONLY method that computes {@code Invoice.tongTien}.
     */
    public static BigDecimal computeTotal(Invoice invoice) {
        BigDecimal sum = BigDecimal.ZERO;
        List<InvoiceServiceItem> items = invoice.getDanhSachDichVu();
        if (items != null) {
            for (InvoiceServiceItem it : items) {
                if (it.getThanhTien() != null) {
                    sum = sum.add(it.getThanhTien());
                }
            }
        }
        if (invoice.getPhiPhat() != null) {
            sum = sum.add(invoice.getPhiPhat());
        }
        return sum;
    }

    /**
     * Pre-save total estimation. Used only for preview before persisting.
     * Builds a temporary invoice, persists line items, then sums them up.
     */
    public static BigDecimal computeTotal(Invoice invoice, InvoiceRequest req, Contract contract) {
        List<InvoiceServiceItem> tempItems = buildItems(invoice, req, contract, null);
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoiceServiceItem it : tempItems) {
            if (it.getThanhTien() != null) sum = sum.add(it.getThanhTien());
        }
        if (req != null && req.getPhiPhat() != null) {
            sum = sum.add(req.getPhiPhat());
        }
        return sum;
    }

    // ====================================================================
    //  Individual component calculators (for pre-save preview)
    // ====================================================================

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
            if (isUtilityServiceName(item.getTenDichVu())) continue;
            BigDecimal qty = item.getSoLuong() != null ? item.getSoLuong() : BigDecimal.ONE;
            total = total.add(item.getDonGia().multiply(qty));
        }
        return total;
    }

    // ====================================================================
    //  Helpers
    // ====================================================================

    /**
     * Creates a line item with automatic amount = qty * unit.
     * Meter readings (chiSoDau, chiSoCuoi) are only set for DIEN and NUOC.
     */
    private static InvoiceServiceItem lineItem(Invoice invoice, String name, String calc,
                                               BigDecimal qty, BigDecimal unit,
                                               Boolean fromContract, String loaiDichVu,
                                               Integer chiSoDau, Integer chiSoCuoi) {
        BigDecimal amount = qty.multiply(unit).setScale(0, RoundingMode.HALF_UP);
        return lineItem(invoice, name, calc, qty, unit, amount, fromContract, loaiDichVu, chiSoDau, chiSoCuoi);
    }

    private static InvoiceServiceItem lineItem(Invoice invoice, String name, String calc,
                                               BigDecimal qty, BigDecimal unit,
                                               BigDecimal amount, Boolean fromContract,
                                               String loaiDichVu,
                                               Integer chiSoDau, Integer chiSoCuoi) {
        InvoiceServiceItem it = new InvoiceServiceItem();
        it.setHoaDon(invoice);
        it.setTenDichVu(name);
        it.setKieuTinh(calc);
        it.setSoLuong(qty);
        it.setDonGia(unit);
        it.setThanhTien(amount);
        it.setLaTuHopDong(fromContract);
        it.setLoaiDichVu(loaiDichVu);
        it.setChiSoDau(chiSoDau);
        it.setChiSoCuoi(chiSoCuoi);
        return it;
    }

    /** Safe integer difference: returns 0 if either value is null, returns 0 if result is negative. */
    private static BigDecimal safeDiff(Integer moi, Integer cu) {
        if (moi == null || cu == null) return BigDecimal.ZERO;
        int diff = moi - cu;
        if (diff < 0) diff = 0;
        return BigDecimal.valueOf(diff);
    }

    /**
     * Returns true if the service name represents electric or water utility
     * (regardless of the {@code laDichVuBoSung} flag). Comparison is
     * case- and accent-insensitive.
     */
    private static boolean isUtilityServiceName(String tenDichVu) {
        if (tenDichVu == null) return false;
        String n = java.text.Normalizer.normalize(tenDichVu, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase(java.util.Locale.ROOT).trim();
        for (String tag : UTILITY_TAGS) {
            if (n.contains(tag)) return true;
        }
        return false;
    }

    private static final String[] UTILITY_TAGS = {
            "dien", "electric", "tiền điện", "tien dien",
            "nuoc", "nước", "water", "tiền nước", "tien nuoc"
    };

    /** Resolves the previous electric meter reading. */
    private static Integer resolveElectricStart(InvoiceRequest req, Invoice previous) {
        if (req != null && req.getChiSoDienCu() != null) return req.getChiSoDienCu();
        if (previous != null && previous.getChiSoDienMoi() != null) return previous.getChiSoDienMoi();
        if (previous != null && previous.getChiSoDienCu() != null) return previous.getChiSoDienCu();
        return null;
    }

    /** Resolves the previous water meter reading. */
    private static Integer resolveWaterStart(InvoiceRequest req, Invoice previous) {
        if (req != null && req.getChiSoNuocCu() != null) return req.getChiSoNuocCu();
        if (previous != null && previous.getChiSoNuocMoi() != null) return previous.getChiSoNuocMoi();
        if (previous != null && previous.getChiSoNuocCu() != null) return previous.getChiSoNuocCu();
        return null;
    }

    /** Resolves water quantity based on billing type. */
    private static BigDecimal resolveWaterQuantity(InvoiceRequest req, Invoice previous,
                                                   WaterCalculationType type, Contract contract) {
        return switch (type) {
            case CHI_SO -> {
                Integer nuocCu = resolveWaterStart(req, previous);
                Integer nuocMoi = req != null ? req.getChiSoNuocMoi() : null;
                yield safeDiff(nuocMoi, nuocCu);
            }
            case THEO_NGUOI -> BigDecimal.valueOf(countPeople(contract));
            case THEO_PHONG -> BigDecimal.ONE;
        };
    }

    /**
     * Resolves quantity for a contract service line item based on its kieuTinh.
     * <ul>
     *   <li>THEO_PHONG / null / unrecognized → 1</li>
     *   <li>THEO_NGUOI → number of people (1 tenant + roommates)</li>
     *   <li>THEO_CHI_SO → meter diff (not used for contract services, defaults to 1)</li>
     * </ul>
     */
    private static BigDecimal resolveContractServiceQuantity(String kieuTinh, Contract contract) {
        if (kieuTinh == null) return BigDecimal.ONE;
        String k = kieuTinh.toLowerCase(java.util.Locale.ROOT).trim();
        if (k.contains("nguoi") || k.contains("người")) {
            return BigDecimal.valueOf(Math.max(countPeople(contract), 1));
        }
        if (k.contains("chi so") || k.contains("chỉ số")) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE;
    }

    /** Counts the actual number of people in the room at billing time. */
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
