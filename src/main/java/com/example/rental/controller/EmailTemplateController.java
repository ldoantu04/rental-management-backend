package com.example.rental.controller;

import com.example.rental.dto.EmailTemplateDTO;
import com.example.rental.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @GetMapping
    public ResponseEntity<List<EmailTemplateDTO>> getAll() {
        return ResponseEntity.ok(emailTemplateService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(emailTemplateService.getById(id));
    }

    @GetMapping("/code/{maMau}")
    public ResponseEntity<EmailTemplateDTO> getByMaMau(@PathVariable String maMau) {
        return ResponseEntity.ok(emailTemplateService.getByMaMau(maMau));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDTO> update(
            @PathVariable Long id,
            @RequestBody EmailTemplateDTO dto) {
        return ResponseEntity.ok(emailTemplateService.update(id, dto));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<EmailTemplateDTO> updateEnabled(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(emailTemplateService.updateEnabled(id, enabled));
    }
}
