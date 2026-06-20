package com.example.rental.service;

import com.example.rental.dto.EmailTemplateDTO;

import java.util.List;

public interface EmailTemplateService {
    List<EmailTemplateDTO> getAll();
    EmailTemplateDTO getById(Long id);
    EmailTemplateDTO getByMaMau(String maMau);
    EmailTemplateDTO update(Long id, EmailTemplateDTO dto);
    EmailTemplateDTO updateEnabled(Long id, Boolean batBuoc);
    void sendWithTemplate(String maMau, String toEmail, java.util.Map<String, String> variables);
}
