package com.example.rental.service.impl;

import com.example.rental.domain.ContractStatus;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.NotificationType;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Notification;
import com.example.rental.model.User;
import com.example.rental.repository.ContractRepository;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.NotificationRepository;
import com.example.rental.service.EmailTemplateService;
import com.example.rental.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private static final int CONTRACT_EXPIRY_DAYS = 30;

    private final NotificationRepository notificationRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailTemplateService emailTemplateService;

    private static final String TEMPLATE_CONTRACT_EXPIRY = "HET_HAN_HD";
    private static final String TEMPLATE_OVERDUE = "QUA_HAN";

    @Override
    @Transactional
    public Notification createNotification(User nguoiDung, NotificationType loai, String tieuDe,
                                            String noiDung, Long maThamChieu) {
        Notification notification = new Notification();
        notification.setNguoiDung(nguoiDung);
        notification.setLoai(loai);
        notification.setTieuDe(tieuDe);
        notification.setNoiDung(noiDung);
        notification.setMaThamChieu(maThamChieu);
        notification.setDaDoc(false);
        notification.setNgayTao(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> findByUser(Long nguoiDungId) {
        return notificationRepository.findByNguoiDungIdOrderByNgayTaoDesc(nguoiDungId);
    }

    @Override
    public List<Notification> findByUserAndType(Long nguoiDungId, NotificationType loai) {
        return notificationRepository.findByNguoiDungIdAndLoaiOrderByNgayTaoDesc(nguoiDungId, loai);
    }

    @Override
    public List<Notification> findUnreadByUser(Long nguoiDungId) {
        return notificationRepository.findByNguoiDungIdAndDaDocOrderByNgayTaoDesc(nguoiDungId, false);
    }

    @Override
    public long countUnreadByUser(Long nguoiDungId) {
        return notificationRepository.countByNguoiDungIdAndDaDoc(nguoiDungId, false);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long id) throws Exception {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay thong bao voi id " + id));
        notification.setDaDoc(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long nguoiDungId) throws Exception {
        List<Notification> unread = notificationRepository.findByNguoiDungIdAndDaDocOrderByNgayTaoDesc(nguoiDungId, false);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setDaDoc(true);
            n.setNgayTao(n.getNgayTao() != null ? n.getNgayTao() : now);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void syncContractExpiryNotifications(User nguoiDung) {
        if (nguoiDung == null) return;
        try {
            List<Contract> all = contractRepository.findAll();
            LocalDate today = LocalDate.now();
            Set<String> existed = new HashSet<>();
            for (Notification n : notificationRepository.findByNguoiDungIdAndLoaiOrderByNgayTaoDesc(
                    nguoiDung.getId(), NotificationType.HOP_DONG_HET_HAN)) {
                if (n.getMaThamChieu() != null) {
                    existed.add("CT-" + n.getMaThamChieu());
                }
            }
            for (Contract contract : all) {
                if (contract.getTrangThai() != ContractStatus.DANG_HIEU_LUC) continue;
                if (contract.getNgayKetThuc() == null) continue;
                LocalDate endDate = contract.getNgayKetThuc();
                long daysLeft = today.until(endDate).getDays();
                if (daysLeft < 0 || daysLeft > CONTRACT_EXPIRY_DAYS) continue;

                String key = "CT-" + contract.getId();
                if (existed.contains(key)) continue;

                String roomLabel = contract.getPhongTro() != null
                        ? contract.getPhongTro().getMaPhong()
                        : "-";
                String tenantName = contract.getKhachThue() != null
                        ? contract.getKhachThue().getHoTen()
                        : "khach thue";
                String tieuDe = "Hop dong sap het han";
                String noiDung = String.format(
                        "Hop dong phong %s cua %s se ket thuc vao ngay %s (con %d ngay).",
                        roomLabel, tenantName, endDate.format(DATE_FMT), daysLeft);

                createNotification(nguoiDung, NotificationType.HOP_DONG_HET_HAN,
                        tieuDe, noiDung, contract.getId());

                com.example.rental.model.Tenant tenant = contract.getKhachThue();
                if (tenant != null && tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
                    com.example.rental.model.Room room = contract.getPhongTro();
                    java.util.Map<String, String> vars = new java.util.HashMap<>();
                    vars.put("tenant_name", tenantName);
                    vars.put("room", roomLabel);
                    vars.put("property", (room != null && room.getNhaTro() != null) ? room.getNhaTro().getTenTro() : "-");
                    vars.put("contract_end_date", endDate.format(DATE_FMT));
                    emailTemplateService.sendWithTemplate(TEMPLATE_CONTRACT_EXPIRY, tenant.getEmail(), vars);
                }
            }
        } catch (Exception e) {
            log.warn("Loi dong bo thong bao hop dong het han: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void syncOverdueInvoiceNotifications(User nguoiDung) {
        if (nguoiDung == null) return;
        try {
            LocalDate today = LocalDate.now();
            Set<String> existed = new HashSet<>();
            for (Notification n : notificationRepository.findByNguoiDungIdAndLoaiOrderByNgayTaoDesc(
                    nguoiDung.getId(), NotificationType.HOA_DON)) {
                if (n.getMaThamChieu() != null && n.getNoiDung() != null
                        && n.getNoiDung().toLowerCase().contains("qua han")) {
                    existed.add("INV-OVERDUE-" + n.getMaThamChieu());
                }
            }
            for (Invoice invoice : invoiceRepository.findAll()) {
                if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) continue;
                if (invoice.getHanThanhToan() == null) continue;
                if (!invoice.getHanThanhToan().isBefore(today)) continue;

                String key = "INV-OVERDUE-" + invoice.getId();
                if (existed.contains(key)) continue;

                Contract contract = invoice.getHopDong();
                String roomLabel = contract != null && contract.getPhongTro() != null
                        ? contract.getPhongTro().getMaPhong()
                        : "-";
                String tenantName = contract != null && contract.getKhachThue() != null
                        ? contract.getKhachThue().getHoTen()
                        : "khach thue";
                long daysOverdue = today.toEpochDay() - invoice.getHanThanhToan().toEpochDay();

                String tieuDe = "Hoa don qua han: " + invoice.getMaHoaDon();
                String noiDung = String.format(
                        "Hoa don phong %s cua %s da qua han %d ngay (han %s, tong tien %s).",
                        roomLabel, tenantName, daysOverdue,
                        invoice.getHanThanhToan().format(DATE_FMT),
                        formatMoney(invoice.getTongTien()));

                createNotification(nguoiDung, NotificationType.HOA_DON,
                        tieuDe, noiDung, invoice.getId());

                com.example.rental.model.Tenant tenant = contract != null ? contract.getKhachThue() : null;
                if (tenant != null && tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
                    com.example.rental.model.Room room = contract.getPhongTro();
                    java.util.Map<String, String> vars = new java.util.HashMap<>();
                    vars.put("tenant_name", tenantName);
                    vars.put("room", roomLabel);
                    vars.put("property", (room != null && room.getNhaTro() != null) ? room.getNhaTro().getTenTro() : "-");
                    vars.put("amount", formatMoney(invoice.getTongTien()));
                    vars.put("due_date", invoice.getHanThanhToan().format(DATE_FMT));
                    vars.put("overdue_days", String.valueOf(daysOverdue));
                    vars.put("late_fee", formatMoney(BigDecimal.ZERO));
                    emailTemplateService.sendWithTemplate(TEMPLATE_OVERDUE, tenant.getEmail(), vars);
                }
            }
        } catch (Exception e) {
            log.warn("Loi dong bo thong bao hoa don qua han: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createSystemNotification(User nguoiDung, String tieuDe, String noiDung, Long maThamChieu) {
        if (nguoiDung == null) return;
        try {
            createNotification(nguoiDung, NotificationType.HE_THONG, tieuDe, noiDung, maThamChieu);
        } catch (Exception e) {
            log.warn("Loi tao thong bao he thong: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void notifyInvoiceCreated(User nguoiDung, Long hoaDonId) {
        try {
            Invoice invoice = invoiceRepository.findById(hoaDonId).orElse(null);
            if (invoice == null) return;
            if (nguoiDung == null) return;

            Contract contract = invoice.getHopDong();
            String roomLabel = contract != null && contract.getPhongTro() != null
                    ? contract.getPhongTro().getMaPhong()
                    : "-";
            String tenantName = contract != null && contract.getKhachThue() != null
                    ? contract.getKhachThue().getHoTen()
                    : "khach thue";
            String month = invoice.getKyHoaDon() != null ? invoice.getKyHoaDon().format(MONTH_FMT) : "-";
            String due = invoice.getHanThanhToan() != null ? invoice.getHanThanhToan().format(DATE_FMT) : "-";
            String amount = formatMoney(invoice.getTongTien());

            String tieuDe = "Hoa don moi: " + invoice.getMaHoaDon();
            String noiDung = String.format(
                    "Hoa don thang %s phong %s cua %s - tong tien %s, han thanh toan %s.",
                    month, roomLabel, tenantName, amount, due);

            createNotification(nguoiDung, NotificationType.HOA_DON, tieuDe, noiDung, invoice.getId());
        } catch (Exception e) {
            log.warn("Loi tao thong bao hoa don moi: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void notifyInvoicePaid(User nguoiDung, Long hoaDonId, String hinhThuc) {
        try {
            Invoice invoice = invoiceRepository.findById(hoaDonId).orElse(null);
            if (invoice == null) return;
            if (invoice.getTrangThai() != InvoiceStatus.DA_THANH_TOAN) return;
            if (nguoiDung == null) return;

            Contract contract = invoice.getHopDong();
            String roomLabel = contract != null && contract.getPhongTro() != null
                    ? contract.getPhongTro().getMaPhong()
                    : "-";
            String tenantName = contract != null && contract.getKhachThue() != null
                    ? contract.getKhachThue().getHoTen()
                    : "khach thue";
            String amount = formatMoney(invoice.getTongTien());
            String method = hinhThuc != null ? hinhThuc : "thanh toan";

            String tieuDe = "Thanh toan thanh cong: " + invoice.getMaHoaDon();
            String noiDung = String.format(
                    "Giao dich %s thanh cong - phong %s, khach thue %s, so tien %s.",
                    method, roomLabel, tenantName, amount);

            createNotification(nguoiDung, NotificationType.THANH_TOAN, tieuDe, noiDung, invoice.getId());
        } catch (Exception e) {
            log.warn("Loi tao thong bao thanh toan: {}", e.getMessage());
        }
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0 VND";
        return MONEY_FMT.format(value) + " VND";
    }
}
