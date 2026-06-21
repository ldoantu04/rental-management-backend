package com.example.rental.controller;

import com.example.rental.dto.EmailTemplateResponse;
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
    public ResponseEntity<List<EmailTemplateResponse>> getAll() {
        return ResponseEntity.ok(emailTemplateService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(emailTemplateService.getById(id));
    }

    @GetMapping("/code/{maMau}")
    public ResponseEntity<EmailTemplateResponse> getByMaMau(@PathVariable String maMau) {
        return ResponseEntity.ok(emailTemplateService.getByMaMau(maMau));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> update(
            @PathVariable Long id,
            @RequestBody EmailTemplateResponse dto) {
        return ResponseEntity.ok(emailTemplateService.update(id, dto));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<EmailTemplateResponse> updateEnabled(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(emailTemplateService.updateEnabled(id, enabled));
    }
}
