package com.example.rental.controller;

import com.example.rental.dto.DashboardFilterResponse;
import com.example.rental.dto.RevenueChartResponse;
import com.example.rental.dto.RoomStatusResponse;
import com.example.rental.model.User;
import com.example.rental.service.DashboardService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueChartResponse>> getRevenueChart(
            @RequestParam int year,
            @RequestParam(required = false) Long motelId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<RevenueChartResponse> data = dashboardService.getRevenueChart(year, motelId, user);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/room-status")
    public ResponseEntity<RoomStatusResponse> getRoomStatus(
            @RequestParam(required = false) Long motelId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        RoomStatusResponse data = dashboardService.getRoomStatus(motelId, user);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/filters")
    public ResponseEntity<DashboardFilterResponse> getFilters(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        DashboardFilterResponse data = dashboardService.getFilters(user);
        return ResponseEntity.ok(data);
    }
}
