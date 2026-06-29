package com.example.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSettingsResponse {
    private BigDecimal defaultDueDay;
    private BigDecimal latePenaltyPercent;
    private boolean autoGenerateInvoice;
    private boolean autoSendOverdueEmail;
    private boolean autoSendPaymentConfirmationEmail;
    private boolean autoSendPaymentReminderEmail;
}
