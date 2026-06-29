package com.example.rental.service;

import java.math.BigDecimal;

public interface SystemSettingService {

    BigDecimal getDefaultDueDay();

    BigDecimal getLatePenaltyPercent();

    boolean isAutoGenerateInvoice();

    boolean isAutoSendOverdueEmail();

    boolean isAutoSendPaymentConfirmationEmail();

    boolean isAutoSendPaymentReminderEmail();

    boolean isAutoSendInvoiceEmail();

    void setDefaultDueDay(BigDecimal value);

    void setLatePenaltyPercent(BigDecimal value);

    void setAutoGenerateInvoice(boolean value);

    void setAutoSendOverdueEmail(boolean value);

    void setAutoSendPaymentConfirmationEmail(boolean value);

    void setAutoSendPaymentReminderEmail(boolean value);

    void setAutoSendInvoiceEmail(boolean value);
}
