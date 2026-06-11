package com.example.rental.controller;

import com.example.rental.dto.OverviewResponse;
import com.example.rental.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewService overviewService;

    @GetMapping
    public ResponseEntity<OverviewResponse> getOverview() {
        OverviewResponse response = overviewService.getOverview();
        return ResponseEntity.ok(response);
    }
}
