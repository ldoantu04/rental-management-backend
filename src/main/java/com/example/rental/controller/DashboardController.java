package com.example.rental.controller;

import com.example.rental.dto.DashboardFilterDTO;
import com.example.rental.dto.RevenueChartDTO;
import com.example.rental.dto.RoomStatusDTO;
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
    public ResponseEntity<List<RevenueChartDTO>> getRevenueChart(
            @RequestParam int year,
            @RequestParam(required = false) Long motelId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<RevenueChartDTO> data = dashboardService.getRevenueChart(year, motelId, user);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/room-status")
    public ResponseEntity<RoomStatusDTO> getRoomStatus(
            @RequestParam(required = false) Long motelId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        RoomStatusDTO data = dashboardService.getRoomStatus(motelId, user);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/filters")
    public ResponseEntity<DashboardFilterDTO> getFilters(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        DashboardFilterDTO data = dashboardService.getFilters(user);
        return ResponseEntity.ok(data);
    }
}
