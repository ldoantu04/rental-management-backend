package com.example.rental.service;

import com.example.rental.config.VNPayConfig;
import com.example.rental.domain.InvoiceStatus;
import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.Transaction;
import com.example.rental.model.User;
import com.example.rental.repository.InvoiceRepository;
import com.example.rental.repository.TransactionRepository;
import com.example.rental.repository.UserRepository;
import com.example.rental.service.EmailTemplateService;
import com.example.rental.service.NotificationService;
import com.example.rental.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final VNPayConfig vnPayConfig;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmailTemplateService emailTemplateService;
    private final SystemSettingService systemSettingService;

    private static final String TEMPLATE_PAYMENT_CONFIRM = "XAC_NHAN_TT";
    private static final java.text.NumberFormat MONEY_FMT =
            java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
    private static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static class PaymentResult {
        private final String code;
        private final String message;
        private final String paymentUrl;

        public PaymentResult(String code, String message, String paymentUrl) {
            this.code = code;
            this.message = message;
            this.paymentUrl = paymentUrl;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getPaymentUrl() { return paymentUrl; }
    }

    public PaymentResult createPayment(String maHoaDon, HttpServletRequest request) throws Exception {
        Invoice invoice = invoiceRepository.findByMaHoaDon(maHoaDon);
        if (invoice == null) {
            return new PaymentResult("01", "The invoice does not exist or has been deleted.", null);
        }
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            return new PaymentResult("02", "The invoice has been paid.", null);
        }
        if (invoice.getTongTien() == null || invoice.getTongTien().signum() <= 0) {
            return new PaymentResult("03", "Tong tien hoa don khong hop le.", null);
        }

        String vnpTxnRef = vnPayConfig.getRandomNumber(8);
        long amount = invoice.getTongTien().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", invoice.getMaHoaDon());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", vnPayConfig.getIpAddress(request));
        vnpParams.put("vnp_CreateDate", formatVnPayDate(0));
        vnpParams.put("vnp_ExpireDate", formatVnPayDate(15));

        String paymentUrl = buildPaymentUrl(vnpParams, vnPayConfig.getPayUrl());
        return new PaymentResult("00", "success", paymentUrl);
    }

    public boolean verifySignature(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) continue;
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) continue;
            fields.put(key, value);
        }
        String signValue = vnPayConfig.hashAllFields(fields);
        String receivedHash = params.get("vnp_SecureHash");
        return signValue.equals(receivedHash);
    }

    @Transactional
    public void processIpn(Map<String, String> params) throws Exception {
        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpAmountStr = params.get("vnp_Amount");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String vnpBankCode = params.get("vnp_BankCode");
        String vnpPayDate = params.get("vnp_PayDate");

        if (vnpTxnRef == null) {
            log.warn("IPN bo qua: thieu vnp_TxnRef");
            return;
        }
        if (transactionRepository.existsByMaGiaoDich(vnpTxnRef)) {
            log.info("IPN da xu ly truoc do: {}", vnpTxnRef);
            return;
        }
        if (!"00".equals(vnpResponseCode)) {
            log.info("IPN giao dich khong thanh cong: ma loi {}", vnpResponseCode);
            return;
        }

        String maHoaDon = extractMaHoaDon(params.get("vnp_OrderInfo"));
        Invoice invoice = maHoaDon != null ? invoiceRepository.findByMaHoaDon(maHoaDon) : null;
        if (invoice == null) {
            log.warn("IPN khong tim thay hoa don cho txnRef {} / orderInfo {}", vnpTxnRef, params.get("vnp_OrderInfo"));
            return;
        }
        if (invoice.getTrangThai() == InvoiceStatus.DA_THANH_TOAN) {
            log.info("IPN hoa don {} da duoc thanh toan truoc do, bo qua", invoice.getMaHoaDon());
            return;
        }

        BigDecimal vnpAmount = new BigDecimal(vnpAmountStr).divide(BigDecimal.valueOf(100));
        BigDecimal expectedAmount = invoice.getTongTien() != null ? invoice.getTongTien() : BigDecimal.ZERO;
        if (vnpAmount.compareTo(expectedAmount) != 0) {
            log.warn("IPN so tien khong khop: nhan {} mong doi {}", vnpAmount, expectedAmount);
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setMaGiaoDich(vnpTxnRef);
        transaction.setHoaDon(invoice);
        transaction.setSoTien(vnpAmount);
        transaction.setHinhThucTT(PaymentMethod.TRUC_TUYEN);
        transaction.setMaCongTT("VNPAY");
        transaction.setDuLieuTT(serializeMeta(vnpBankCode, vnpTransactionNo, vnpPayDate));
        transaction.setTrangThai(PaymentStatus.THANH_CONG);
        transaction.setNgayThanhToan(LocalDateTime.now());
        transaction.setNgayTao(LocalDateTime.now());
        transaction.setGhiChu("VNPay thanh toan hoa don " + invoice.getMaHoaDon());
        transactionRepository.save(transaction);

        invoice.setTrangThai(InvoiceStatus.DA_THANH_TOAN);
        invoice.setNgaySua(LocalDateTime.now());
        invoiceRepository.save(invoice);
        log.info("IPN thanh cong: hoa don {} da duoc cap nhat PAID", invoice.getMaHoaDon());
        User owner = resolveOwner(invoice);
        notificationService.notifyInvoicePaid(owner, invoice.getId(), "VNPay");
        if (systemSettingService.isAutoSendPaymentConfirmationEmail()) {
            sendPaymentConfirmationEmail(invoice, "VNPay");
        }
    }

    @Transactional
    public void processReturn(Map<String, String> params) throws Exception {
        if (!"00".equals(params.get("vnp_ResponseCode"))) {
            return;
        }
        if (transactionRepository.existsByMaGiaoDich(params.get("vnp_TxnRef"))) {
            return;
        }
        processIpn(params);
    }

    public Invoice findInvoiceByTxnRef(String txnRef) {
        if (txnRef == null) return null;
        List<Transaction> all = transactionRepository.findAll();
        for (Transaction t : all) {
            if (txnRef.equals(t.getMaGiaoDich())) {
                return t.getHoaDon();
            }
        }
        return null;
    }

    private String extractMaHoaDon(String orderInfo) {
        if (orderInfo == null) return null;
        String[] tokens = orderInfo.split("\\s+");
        for (String token : tokens) {
            if (token.startsWith("HD") || token.startsWith("INV")) {
                return token;
            }
        }
        return orderInfo.trim();
    }

    private String buildPaymentUrl(Map<String, String> params, String baseUrl) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String secureHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return baseUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private String formatVnPayDate(int addMinutes) {
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        if (addMinutes != 0) {
            cld.add(Calendar.MINUTE, addMinutes);
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(cld.getTime());
    }

    private String serializeMeta(String bankCode, String transactionNo, String payDate) {
        StringBuilder sb = new StringBuilder();
        if (bankCode != null) sb.append("Bank:").append(bankCode).append(';');
        if (transactionNo != null) sb.append("VnpTxn:").append(transactionNo).append(';');
        if (payDate != null) sb.append("PayDate:").append(payDate).append(';');
        return sb.toString();
    }

    private User resolveOwner(Invoice invoice) {
        if (invoice == null) return null;
        if (invoice.getNguoiTao() != null) return invoice.getNguoiTao();
        Contract contract = invoice.getHopDong();
        if (contract != null && contract.getNguoiTao() != null) return contract.getNguoiTao();
        if (contract != null && contract.getPhongTro() != null) {
            Room room = contract.getPhongTro();
            Motel motel = room.getNhaTro();
            if (motel != null && motel.getNguoiTao() != null) return motel.getNguoiTao();
        }
        List<User> users = userRepository.findAll();
        return users.isEmpty() ? null : users.get(0);
    }

    private void sendPaymentConfirmationEmail(Invoice invoice, String method) {
        try {
            com.example.rental.model.Contract contract = invoice.getHopDong();
            com.example.rental.model.Tenant tenant = contract != null ? contract.getKhachThue() : null;
            com.example.rental.model.Room room = contract != null ? contract.getPhongTro() : null;

            if (tenant == null || tenant.getEmail() == null || tenant.getEmail().isBlank()) {
                return;
            }

            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("tenant_name", tenant.getHoTen() != null ? tenant.getHoTen() : "Quy khach");
            vars.put("room", room != null ? room.getMaPhong() : "-");
            vars.put("property", (room != null && room.getNhaTro() != null) ? room.getNhaTro().getTenTro() : "-");
            vars.put("amount", invoice.getTongTien() != null ? MONEY_FMT.format(invoice.getTongTien()) : "0");
            vars.put("payment_method", method != null ? method : "Thanh toan");
            vars.put("payment_date", LocalDateTime.now().format(DATE_FMT));

            emailTemplateService.sendWithTemplate(TEMPLATE_PAYMENT_CONFIRM, tenant.getEmail(), vars);
        } catch (Exception e) {
            log.error("Loi gui email xac nhan thanh toan: {}", e.getMessage());
        }
    }
}
