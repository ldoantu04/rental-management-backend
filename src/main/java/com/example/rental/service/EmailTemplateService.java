package com.example.rental.service;

import com.example.rental.dto.EmailTemplateResponse;

import java.util.List;

public interface EmailTemplateService {
    List<EmailTemplateResponse> getAll();
    EmailTemplateResponse getById(Long id);
    EmailTemplateResponse getByMaMau(String maMau);
    EmailTemplateResponse update(Long id, EmailTemplateResponse dto);
    EmailTemplateResponse updateEnabled(Long id, Boolean batBuoc);
    void sendWithTemplate(String maMau, String toEmail, java.util.Map<String, String> variables);
}
