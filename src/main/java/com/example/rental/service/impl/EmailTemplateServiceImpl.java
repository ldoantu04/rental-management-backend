package com.example.rental.service.impl;

import com.example.rental.dto.EmailTemplateDTO;
import com.example.rental.model.EmailTemplate;
import com.example.rental.repository.EmailTemplateRepository;
import com.example.rental.service.EmailService;
import com.example.rental.service.EmailTemplateService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailService emailService;

    private static final String MA_NHAC_THANH_TOAN = "NHAC_THANH_TOAN";
    private static final String MA_QUA_HAN = "QUA_HAN";
    private static final String MA_XAC_NHAN_TT = "XAC_NHAN_TT";
    private static final String MA_HET_HAN_HD = "HET_HAN_HD";

    private static EmailTemplate createTemplate(String maMau, String tenMau, String tieuDe, String noiDung, boolean batBuoc) {
        EmailTemplate t = new EmailTemplate();
        t.setMaMau(maMau);
        t.setTenMau(tenMau);
        t.setTieuDe(tieuDe);
        t.setNoiDung(noiDung);
        t.setBatBuoc(batBuoc);
        return t;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultTemplates() {
        if (emailTemplateRepository.count() > 0) {
            return;
        }

        List<EmailTemplate> defaults = List.of(
            createTemplate(MA_NHAC_THANH_TOAN, "Nhắc nhở thanh toán",
                "[SmartRental] Hóa đơn tiền thuê {month} - {invoice_code}",
                "SmartRental\nHệ thống quản lý nhà trọ thông minh\n\nXin chào {tenant_name},\n\nHóa đơn cho tháng {month} của phòng {room} - {property} sẽ đến hạn thanh toán vào ngày {due_date} với tổng số tiền {amount} VND.\n\nVui lòng truy cập các liên kết bên dưới để xem chi tiết và thanh toán hóa đơn.\n\nLiên kết xem hóa đơn:\n{invoice_url}\n\nLiên kết thanh toán:\n{payment_url}\n\nMã hóa đơn:\n{invoice_code}\n\nTrân trọng,\n\nĐội ngũ SmartRental",
                true),
            createTemplate(MA_QUA_HAN, "Thông báo quá hạn",
                "[SmartRental] Hóa đơn quá hạn thanh toán - {invoice_code}",
                "Xin chào {tenant_name},\n\nHóa đơn của bạn tại phòng {room} - {property} đã quá hạn thanh toán.\n\nSố tiền cần thanh toán:\n{amount} VND\n\nNgày đến hạn:\n{due_date}\n\nSố ngày quá hạn:\n{overdue_days}\n\nPhí trả chậm:\n{late_fee}\n\nVui lòng thanh toán trong thời gian sớm nhất để tránh phát sinh thêm chi phí.\n\nTrân trọng,\n\nĐội ngũ SmartRental",
                true),
            createTemplate(MA_XAC_NHAN_TT, "Xác nhận thanh toán",
                "[SmartRental] Xác nhận thanh toán thành công",
                "Xin chào {tenant_name},\n\nSmartRental đã ghi nhận thanh toán thành công cho hóa đơn của bạn.\n\nPhòng:\n{room}\n\nNhà trọ:\n{property}\n\nSố tiền:\n{amount} VND\n\nPhương thức thanh toán:\n{payment_method}\n\nNgày thanh toán:\n{payment_date}\n\nXin cảm ơn bạn.\n\nTrân trọng,\n\nĐội ngũ SmartRental",
                true),
            createTemplate(MA_HET_HAN_HD, "Hết hạn hợp đồng",
                "[SmartRental] Hợp đồng sắp hết hạn",
                "Xin chào {tenant_name},\n\nHợp đồng thuê phòng {room} tại {property} sẽ hết hạn vào ngày {contract_end_date}.\n\nNếu bạn muốn tiếp tục thuê phòng, vui lòng liên hệ quản lý để gia hạn hợp đồng.\n\nTrân trọng,\n\nĐội ngũ SmartRental",
                true)
        );

        emailTemplateRepository.saveAll(defaults);
        log.info("Da khoi tao {} mau email mac dinh", defaults.size());
    }

    @Override
    public List<EmailTemplateDTO> getAll() {
        return emailTemplateRepository.findAllByOrderByIdAsc().stream()
                .map(toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmailTemplateDTO getById(Long id) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay mau email voi id " + id));
        return toDTO.apply(template);
    }

    @Override
    public EmailTemplateDTO getByMaMau(String maMau) {
        EmailTemplate template = emailTemplateRepository.findByMaMau(maMau)
                .orElseThrow(() -> new RuntimeException("Khong tim thay mau email: " + maMau));
        return toDTO.apply(template);
    }

    @Override
    @Transactional
    public EmailTemplateDTO update(Long id, EmailTemplateDTO dto) {
        if (dto.getTieuDe() == null || dto.getTieuDe().isBlank()) {
            throw new IllegalArgumentException("Tieu de khong duoc de trong");
        }
        if (dto.getNoiDung() == null || dto.getNoiDung().isBlank()) {
            throw new IllegalArgumentException("Noi dung khong duoc de trong");
        }

        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay mau email voi id " + id));
        template.setTieuDe(dto.getTieuDe());
        template.setNoiDung(dto.getNoiDung());
        EmailTemplate saved = emailTemplateRepository.save(template);
        return toDTO.apply(saved);
    }

    @Override
    @Transactional
    public EmailTemplateDTO updateEnabled(Long id, Boolean batBuoc) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay mau email voi id " + id));
        template.setBatBuoc(batBuoc);
        EmailTemplate saved = emailTemplateRepository.save(template);
        return toDTO.apply(saved);
    }

    @Override
    public void sendWithTemplate(String maMau, String toEmail, Map<String, String> variables) {
        EmailTemplate template = emailTemplateRepository.findByMaMau(maMau).orElse(null);
        if (template == null) {
            log.warn("Mau email khong ton tai: {}", maMau);
            return;
        }
        if (!Boolean.TRUE.equals(template.getBatBuoc())) {
            log.info("Mau email {} hien dang tat, bo qua gui", maMau);
            return;
        }

        String subject = replaceVariables(template.getTieuDe(), variables);
        String content = replaceVariables(template.getNoiDung(), variables);

        try {
            emailService.sendInvoiceEmail(toEmail, subject, wrapPlainText(content));
            log.info("Da gui email {} den {}", maMau, toEmail);
        } catch (MessagingException e) {
            log.error("Loi gui email {}: {}", maMau, e.getMessage());
        }
    }

    private String replaceVariables(String text, Map<String, String> variables) {
        if (text == null || variables == null) return text;
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private String wrapPlainText(String text) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #111827;\">"
                + "<pre style=\"white-space: pre-wrap; word-wrap: break-word; font-family: Arial, sans-serif; font-size: 14px; line-height: 1.7; color: #374151;\">"
                + text.replace("\n", "<br/>")
                + "</pre></div>";
    }

    private final java.util.function.Function<EmailTemplate, EmailTemplateDTO> toDTO = t -> EmailTemplateDTO.builder()
            .id(t.getId())
            .maMau(t.getMaMau())
            .tenMau(t.getTenMau())
            .tieuDe(t.getTieuDe())
            .noiDung(t.getNoiDung())
            .batBuoc(t.getBatBuoc())
            .build();
}
