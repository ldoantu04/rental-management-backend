package com.example.rental.service.impl;

import com.example.rental.model.SystemSetting;
import com.example.rental.repository.SystemSettingRepository;
import com.example.rental.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public static final String KEY_DEFAULT_DUE_DAY = "INVOICE_DEFAULT_DUE_DAY";
    public static final String KEY_LATE_PENALTY_PERCENT = "INVOICE_LATE_PENALTY_PERCENT";
    public static final String KEY_AUTO_GENERATE_INVOICE = "INVOICE_AUTO_GENERATE";
    public static final String KEY_AUTO_SEND_OVERDUE_EMAIL = "INVOICE_AUTO_SEND_OVERDUE_EMAIL";
    public static final String KEY_AUTO_SEND_PAYMENT_CONFIRMATION = "INVOICE_AUTO_SEND_PAYMENT_CONFIRMATION";
    public static final String KEY_AUTO_SEND_PAYMENT_REMINDER = "INVOICE_AUTO_SEND_PAYMENT_REMINDER";
    public static final String KEY_AUTO_SEND_INVOICE_EMAIL = "INVOICE_AUTO_SEND_INVOICE_EMAIL";

    @PostConstruct
    @Transactional
    public void seedDefaultSettings() {
        seed(KEY_DEFAULT_DUE_DAY, "5", "Han thanh toan mac dinh (ngay trong thang)");
        seed(KEY_LATE_PENALTY_PERCENT, "10", "Phan tram phi phat tre han");
        seed(KEY_AUTO_GENERATE_INVOICE, "true", "Tu dong tao hoa don hang thang");
        seed(KEY_AUTO_SEND_OVERDUE_EMAIL, "true", "Tu dong gui email qua han");
        seed(KEY_AUTO_SEND_PAYMENT_CONFIRMATION, "true", "Tu dong gui email xac nhan thanh toan");
        seed(KEY_AUTO_SEND_PAYMENT_REMINDER, "true", "Tu dong gui email nhac thanh toan");
        seed(KEY_AUTO_SEND_INVOICE_EMAIL, "true", "Tu dong gui email khi tao hoa don");
    }

    private void seed(String key, String defaultValue, String description) {
        if (!systemSettingRepository.existsByMaCaiDat(key)) {
            SystemSetting setting = new SystemSetting();
            setting.setMaCaiDat(key);
            setting.setGiaTri(defaultValue);
            setting.setMoTa(description);
            systemSettingRepository.save(setting);
            log.info("Da khoi tao cai dat: {} = {}", key, defaultValue);
        }
    }

    private String getValue(String key) {
        return systemSettingRepository.findByMaCaiDat(key)
                .map(SystemSetting::getGiaTri)
                .orElse(null);
    }

    private void setValue(String key, String value) {
        SystemSetting setting = systemSettingRepository.findByMaCaiDat(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setMaCaiDat(key);
                    return s;
                });
        setting.setGiaTri(value);
        setting.setNgaySua(java.time.LocalDateTime.now());
        systemSettingRepository.save(setting);
    }

    @Override
    public BigDecimal getDefaultDueDay() {
        String val = getValue(KEY_DEFAULT_DUE_DAY);
        try {
            return val != null ? new BigDecimal(val) : new BigDecimal("5");
        } catch (NumberFormatException e) {
            return new BigDecimal("5");
        }
    }

    @Override
    public BigDecimal getLatePenaltyPercent() {
        String val = getValue(KEY_LATE_PENALTY_PERCENT);
        try {
            return val != null ? new BigDecimal(val) : new BigDecimal("10");
        } catch (NumberFormatException e) {
            return new BigDecimal("10");
        }
    }

    @Override
    public boolean isAutoGenerateInvoice() {
        return Boolean.parseBoolean(getValue(KEY_AUTO_GENERATE_INVOICE));
    }

    @Override
    public boolean isAutoSendOverdueEmail() {
        return Boolean.parseBoolean(getValue(KEY_AUTO_SEND_OVERDUE_EMAIL));
    }

    @Override
    public boolean isAutoSendPaymentConfirmationEmail() {
        return Boolean.parseBoolean(getValue(KEY_AUTO_SEND_PAYMENT_CONFIRMATION));
    }

    @Override
    public boolean isAutoSendPaymentReminderEmail() {
        return Boolean.parseBoolean(getValue(KEY_AUTO_SEND_PAYMENT_REMINDER));
    }

    @Override
    public boolean isAutoSendInvoiceEmail() {
        return Boolean.parseBoolean(getValue(KEY_AUTO_SEND_INVOICE_EMAIL));
    }

    @Override
    @Transactional
    public void setDefaultDueDay(BigDecimal value) {
        setValue(KEY_DEFAULT_DUE_DAY, value != null ? value.toString() : "5");
    }

    @Override
    @Transactional
    public void setLatePenaltyPercent(BigDecimal value) {
        setValue(KEY_LATE_PENALTY_PERCENT, value != null ? value.toString() : "10");
    }

    @Override
    @Transactional
    public void setAutoGenerateInvoice(boolean value) {
        setValue(KEY_AUTO_GENERATE_INVOICE, String.valueOf(value));
    }

    @Override
    @Transactional
    public void setAutoSendOverdueEmail(boolean value) {
        setValue(KEY_AUTO_SEND_OVERDUE_EMAIL, String.valueOf(value));
    }

    @Override
    @Transactional
    public void setAutoSendPaymentConfirmationEmail(boolean value) {
        setValue(KEY_AUTO_SEND_PAYMENT_CONFIRMATION, String.valueOf(value));
    }

    @Override
    @Transactional
    public void setAutoSendPaymentReminderEmail(boolean value) {
        setValue(KEY_AUTO_SEND_PAYMENT_REMINDER, String.valueOf(value));
    }

    @Override
    @Transactional
    public void setAutoSendInvoiceEmail(boolean value) {
        setValue(KEY_AUTO_SEND_INVOICE_EMAIL, String.valueOf(value));
    }
}
