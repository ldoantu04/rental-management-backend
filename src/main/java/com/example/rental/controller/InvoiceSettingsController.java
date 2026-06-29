package com.example.rental.controller;

import com.example.rental.dto.InvoiceSettingsResponse;
import com.example.rental.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice-settings")
@RequiredArgsConstructor
public class InvoiceSettingsController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<InvoiceSettingsResponse> getSettings() {
        InvoiceSettingsResponse dto = new InvoiceSettingsResponse(
                systemSettingService.getDefaultDueDay(),
                systemSettingService.getLatePenaltyPercent(),
                systemSettingService.isAutoGenerateInvoice(),
                systemSettingService.isAutoSendOverdueEmail(),
                systemSettingService.isAutoSendPaymentConfirmationEmail(),
                systemSettingService.isAutoSendPaymentReminderEmail()
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<InvoiceSettingsResponse> updateSettings(@RequestBody InvoiceSettingsResponse dto) {
        if (dto.getDefaultDueDay() != null) {
            systemSettingService.setDefaultDueDay(dto.getDefaultDueDay());
        }
        if (dto.getLatePenaltyPercent() != null) {
            systemSettingService.setLatePenaltyPercent(dto.getLatePenaltyPercent());
        }
        systemSettingService.setAutoGenerateInvoice(dto.isAutoGenerateInvoice());
        systemSettingService.setAutoSendOverdueEmail(dto.isAutoSendOverdueEmail());
        systemSettingService.setAutoSendPaymentConfirmationEmail(dto.isAutoSendPaymentConfirmationEmail());
        systemSettingService.setAutoSendPaymentReminderEmail(dto.isAutoSendPaymentReminderEmail());

        InvoiceSettingsResponse result = new InvoiceSettingsResponse(
                systemSettingService.getDefaultDueDay(),
                systemSettingService.getLatePenaltyPercent(),
                systemSettingService.isAutoGenerateInvoice(),
                systemSettingService.isAutoSendOverdueEmail(),
                systemSettingService.isAutoSendPaymentConfirmationEmail(),
                systemSettingService.isAutoSendPaymentReminderEmail()
        );
        return ResponseEntity.ok(result);
    }
}
