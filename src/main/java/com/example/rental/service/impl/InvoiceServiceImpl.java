package com.example.rental.service.impl;

import com.example.rental.config.VNPayConfig;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.WaterCalculationType;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.dto.InvoiceServiceItemRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.InvoiceServiceItemRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.repository.TenantRepository;
import com.example.rental.service.EmailService;
import com.example.rental.service.InvoiceService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final InvoiceServiceItemRepository invoiceServiceItemRepository;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final EmailService emailService;
    private final VNPayConfig vnPayConfig;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    @Transactional
    public Invoice createInvoice(InvoiceRequest req) throws Exception {
        Contract contract = contractRepository.findById(req.getMaHopDong())
                .orElseThrow(() -> new Exception("Khong tim thay hop dong voi id " + req.getMaHopDong()));

        long count = invoiceRepository.count();
        String maHoaDon = "HD" + String.format("%03d", count + 1);

        BigDecimal tongTien = req.getTongTien() != null
                ? req.getTongTien()
                : computeTotal(contract, req);

        Invoice invoice = new Invoice();
        invoice.setMaHoaDon(maHoaDon);
        invoice.setHopDong(contract);
        invoice.setKyHoaDon(req.getKyHoaDon());
        invoice.setChiSoDienCu(req.getChiSoDienCu());
        invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        invoice.setGiaDien(req.getGiaDien());
        invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        invoice.setGiaNuoc(req.getGiaNuoc());
        if (req.getKieuTinhNuoc() != null) {
            invoice.setKieuTinhNuoc(req.getKieuTinhNuoc());
        }
        invoice.setTienPhong(req.getTienPhong());
        invoice.setTongTien(tongTien);
        invoice.setHanThanhToan(req.getHanThanhToan());
        invoice.setTrangThai(InvoiceStatus.CHUA_THANH_TOAN);
        invoice.setGhiChu(req.getGhiChu());
        invoice.setNgayTao(LocalDateTime.now());
        invoice.setNgaySua(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        syncServiceItems(saved, req.getDanhSachDichVu());
        sendInvoiceNotification(saved);
        return saved;
    }

    @Override
    @Transactional
    public Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception {
        Invoice invoice = findById(id);

        if (invoice.getTrangThai() != InvoiceStatus.CHUA_THANH_TOAN) {
            throw new Exception("Hoa don da thanh toan, khong the cap nhat");
        }

        if (req.getKyHoaDon() != null) {
            invoice.setKyHoaDon(req.getKyHoaDon());
        }
        if (req.getChiSoDienCu() != null) {
            invoice.setChiSoDienCu(req.getChiSoDienCu());
        }
        if (req.getChiSoDienMoi() != null) {
            invoice.setChiSoDienMoi(req.getChiSoDienMoi());
        }
        if (req.getGiaDien() != null) {
            invoice.setGiaDien(req.getGiaDien());
        }
        if (req.getChiSoNuocCu() != null) {
            invoice.setChiSoNuocCu(req.getChiSoNuocCu());
        }
        if (req.getChiSoNuocMoi() != null) {
            invoice.setChiSoNuocMoi(req.getChiSoNuocMoi());
        }
        if (req.getGiaNuoc() != null) {
            invoice.setGiaNuoc(req.getGiaNuoc());
        }
        if (req.getKieuTinhNuoc() != null) {
            invoice.setKieuTinhNuoc(req.getKieuTinhNuoc());
        }
        if (req.getTienPhong() != null) {
            invoice.setTienPhong(req.getTienPhong());
        }
        Contract contract = invoice.getHopDong();
        if (req.getTongTien() != null) {
            invoice.setTongTien(req.getTongTien());
        } else {
            invoice.setTongTien(computeTotal(contract, req));
        }
        if (req.getHanThanhToan() != null) {
            invoice.setHanThanhToan(req.getHanThanhToan());
        }
        if (req.getTrangThai() != null) {
            invoice.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            invoice.setGhiChu(req.getGhiChu());
        }
        invoice.setNgaySua(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        if (req.getDanhSachDichVu() != null) {
            syncServiceItems(saved, req.getDanhSachDichVu());
        }
        return saved;
    }

    private void syncServiceItems(Invoice invoice, List<InvoiceServiceItemRequest> requests) {
        if (requests == null) return;
        invoice.getDanhSachDichVu().clear();
        invoiceServiceItemRepository.flush();

        for (InvoiceServiceItemRequest req : requests) {
            if (req.getTenDichVu() == null || req.getTenDichVu().trim().isEmpty()) {
                continue;
            }
            BigDecimal soLuong = req.getSoLuong() != null ? req.getSoLuong() : BigDecimal.ONE;
            BigDecimal donGia = req.getDonGia() != null ? req.getDonGia() : BigDecimal.ZERO;
            BigDecimal thanhTien = soLuong.multiply(donGia).setScale(0, RoundingMode.HALF_UP);

            InvoiceServiceItem item = new InvoiceServiceItem();
            item.setHoaDon(invoice);
            item.setTenDichVu(req.getTenDichVu());
            item.setKieuTinh(req.getKieuTinh() != null ? req.getKieuTinh() : "Theo phong");
            item.setSoLuong(soLuong);
            item.setDonGia(donGia);
            item.setThanhTien(thanhTien);
            item.setLaTuHopDong(req.getLaTuHopDong() == null ? Boolean.TRUE : req.getLaTuHopDong());
            invoice.getDanhSachDichVu().add(item);
        }
        invoiceServiceItemRepository.saveAll(invoice.getDanhSachDichVu());
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id) throws Exception {
        Invoice invoice = findById(id);

        if (invoice.getTrangThai() != InvoiceStatus.CHUA_THANH_TOAN) {
            throw new Exception("Hoa don da thanh toan, khong the xoa");
        }

        invoiceServiceItemRepository.deleteByHoaDonId(invoice.getId());
        invoiceRepository.delete(invoice);
    }

    @Override
    public Invoice findById(Long id) throws Exception {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay hoa don voi id " + id));
    }

    @Override
    public Invoice findByMaHoaDon(String maHoaDon) throws Exception {
        Invoice invoice = invoiceRepository.findByMaHoaDon(maHoaDon);
        if (invoice == null) {
            throw new Exception("Khong tim thay hoa don voi ma " + maHoaDon);
        }
        return invoice;
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    @Override
    public List<Invoice> findByHopDongId(Long hopDongId) {
        List<Invoice> all = invoiceRepository.findAll();
        return all.stream()
                .filter(i -> i.getHopDong() != null && i.getHopDong().getId().equals(hopDongId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findByTrangThai(InvoiceStatus trangThai) {
        return invoiceRepository.findByTrangThai(trangThai);
    }

    @Override
    public List<Invoice> search(String keyword, InvoiceStatus trangThai) {
        List<Invoice> invoices = invoiceRepository.findAll();
        return invoices.stream()
                .filter(i -> keyword == null || keyword.isBlank()
                        || (i.getMaHoaDon() != null && i.getMaHoaDon().toLowerCase().contains(keyword.toLowerCase())))
                .filter(i -> trangThai == null || i.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Invoice markAsPaid(Long id) throws Exception {
        Invoice invoice = findById(id);
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            throw new Exception("Hoa don da duoc thanh toan truoc do");
        }
        invoice.setTrangThai(InvoiceStatus.DA_THANH_TOAN);
        invoice.setNgaySua(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice findLatestByHopDongId(Long hopDongId) {
        if (hopDongId == null) return null;
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getHopDong() != null && i.getHopDong().getId().equals(hopDongId))
                .max((a, b) -> {
                    if (a.getKyHoaDon() == null && b.getKyHoaDon() == null) return 0;
                    if (a.getKyHoaDon() == null) return -1;
                    if (b.getKyHoaDon() == null) return 1;
                    return a.getKyHoaDon().compareTo(b.getKyHoaDon());
                })
                .orElse(null);
    }

    public BigDecimal computeElectricAmount(Contract contract, InvoiceRequest req) {
        if (req.getChiSoDienMoi() == null || req.getChiSoDienCu() == null || req.getGiaDien() == null) {
            return BigDecimal.ZERO;
        }
        int diff = Math.max(req.getChiSoDienMoi() - req.getChiSoDienCu(), 0);
        return req.getGiaDien().multiply(BigDecimal.valueOf(diff));
    }

    public BigDecimal computeWaterAmount(Contract contract, InvoiceRequest req) {
        if (req.getGiaNuoc() == null) return BigDecimal.ZERO;
        WaterCalculationType type = req.getKieuTinhNuoc() != null
                ? req.getKieuTinhNuoc()
                : (contract != null ? WaterCalculationType.CHI_SO : WaterCalculationType.CHI_SO);
        return switch (type) {
            case THEO_PHONG -> req.getGiaNuoc();
            case THEO_NGUOI -> {
                int people = countPeopleInRoom(contract);
                yield req.getGiaNuoc().multiply(BigDecimal.valueOf(Math.max(people, 1)));
            }
            case CHI_SO -> {
                if (req.getChiSoNuocMoi() == null || req.getChiSoNuocCu() == null) {
                    yield BigDecimal.ZERO;
                }
                int diff = Math.max(req.getChiSoNuocMoi() - req.getChiSoNuocCu(), 0);
                yield req.getGiaNuoc().multiply(BigDecimal.valueOf(diff));
            }
        };
    }

    private int countPeopleInRoom(Contract contract) {
        if (contract == null || contract.getPhongTro() == null) return 1;
        Room room = contract.getPhongTro();
        int count = 0;
        try {
            if (room.getSoNguoi() != null && room.getSoNguoi() > 0) {
                count = room.getSoNguoi();
            }
        } catch (Exception ignored) {
        }
        if (count == 0) {
            Tenant tenant = contract.getKhachThue();
            if (tenant != null) {
                int roommates = tenant.getDanhSachNguoiOCung() != null ? tenant.getDanhSachNguoiOCung().size() : 0;
                count = 1 + roommates;
            }
        }
        return Math.max(count, 1);
    }

    public BigDecimal computeServiceAmount(InvoiceRequest req) {
        if (req.getDanhSachDichVu() == null || req.getDanhSachDichVu().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceServiceItemRequest item : req.getDanhSachDichVu()) {
            if (item.getDonGia() == null) continue;
            BigDecimal soLuong = item.getSoLuong() != null ? item.getSoLuong() : BigDecimal.ONE;
            total = total.add(item.getDonGia().multiply(soLuong));
        }
        return total;
    }

    public BigDecimal computeTotal(Contract contract, InvoiceRequest req) {
        BigDecimal tong = BigDecimal.ZERO;
        if (req.getTienPhong() != null) {
            tong = tong.add(req.getTienPhong());
        }
        tong = tong.add(computeElectricAmount(contract, req));
        tong = tong.add(computeWaterAmount(contract, req));
        tong = tong.add(computeServiceAmount(req));
        return tong;
    }

    public int countPeopleByRoomId(Long roomId) {
        if (roomId == null) return 1;
        try {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null) return 1;
            if (room.getSoNguoi() != null && room.getSoNguoi() > 0) {
                return room.getSoNguoi();
            }
        } catch (Exception ignored) {
        }
        try {
            List<Tenant> tenants = tenantRepository.findByPhongTroId(roomId);
            if (!tenants.isEmpty()) {
                int total = 0;
                for (Tenant t : tenants) {
                    total += 1;
                    if (t.getDanhSachNguoiOCung() != null) {
                        total += t.getDanhSachNguoiOCung().size();
                    }
                }
                return Math.max(total, 1);
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private void sendInvoiceNotification(Invoice invoice) {
        try {
            Contract contract = invoice.getHopDong();
            Tenant tenant = contract != null ? contract.getKhachThue() : null;
            Room room = contract != null ? contract.getPhongTro() : null;

            if (tenant == null || tenant.getEmail() == null || tenant.getEmail().isBlank()) {
                log.warn("Khong the gui email: khach thue hoac email trong");
                return;
            }

            String roomLabel = room != null
                    ? room.getMaPhong() + (room.getNhaTro() != null ? " - " + room.getNhaTro().getTenTro() : "")
                    : "-";
            String tenantName = tenant.getHoTen() != null ? tenant.getHoTen() : "Quy khach";
            String month = invoice.getKyHoaDon() != null ? invoice.getKyHoaDon().format(MONTH_FMT) : "-";
            String dueDate = invoice.getHanThanhToan() != null ? invoice.getHanThanhToan().format(DATE_FMT) : "-";
            String total = invoice.getTongTien() != null ? MONEY_FMT.format(invoice.getTongTien()) + " VND" : "0 VND";
            String viewUrl = vnPayConfig.getPublicBaseUrl() + "/api/invoices/" + invoice.getId() + "/pdf";
            String payUrl = vnPayConfig.getPublicBaseUrl() + "/pay/" + invoice.getMaHoaDon();

            String html = String.format(
                    "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #111827;\">"
                            + "<h2 style=\"color: #80001C; margin-bottom: 8px;\">SmartRental</h2>"
                            + "<p style=\"color: #6B7280; margin-top: 0;\">He thong quan ly nha tro thong minh</p>"
                            + "<hr style=\"border: 0; border-top: 1px solid #E5E7EB; margin: 16px 0;\"/>"
                            + "<p>Xin chao <b>%s</b>,</p>"
                            + "<p>Hoa don cho thang <b>%s</b> cua phong <b>%s</b> co han thanh toan vao ngay <b>%s</b> voi tong tien <b>%s</b>.</p>"
                            + "<p>Vui long truy cap cac lien ket ben duoi de xem chi tiet va thanh toan hoa don:</p>"
                            + "<p style=\"margin: 16px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:10px 18px;background:#1F2937;color:#fff;text-decoration:none;border-radius:8px;margin-right:8px;\">Lien ket xem hoa don</a>"
                            + "<a href=\"%s\" style=\"display:inline-block;padding:10px 18px;background:#80001C;color:#fff;text-decoration:none;border-radius:8px;\">Thanh toan ngay</a></p>"
                            + "<p style=\"color: #6B7280; font-size: 13px;\">Ma hoa don: <b>%s</b></p>"
                            + "<hr style=\"border: 0; border-top: 1px solid #E5E7EB; margin: 24px 0 12px;\"/>"
                            + "<p style=\"color: #6B7280; font-size: 12px;\">Tran trong,<br/>Doi ngu SmartRental</p>"
                            + "</div>",
                    tenantName, month, roomLabel, dueDate, total, viewUrl, payUrl, invoice.getMaHoaDon()
            );

            String subject = "[SmartRental] Hoa don tien thue " + month + " - " + invoice.getMaHoaDon();
            emailService.sendInvoiceEmail(tenant.getEmail(), subject, html);
        } catch (MessagingException e) {
            log.error("Loi gui email hoa don: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Loi khi gui thong bao hoa don: {}", e.getMessage());
        }
    }
}
