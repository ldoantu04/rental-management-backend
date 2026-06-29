package com.example.rental.service;

import com.example.rental.dto.DashboardFilterResponse;
import com.example.rental.dto.OverviewResponse;
import com.example.rental.dto.RevenueChartResponse;
import com.example.rental.dto.RoomStatusResponse;
import com.example.rental.model.User;

import java.util.List;

public interface DashboardService {
    OverviewResponse getOverview();
    List<RevenueChartResponse> getRevenueChart(int year, Long motelId, User currentUser);
    RoomStatusResponse getRoomStatus(Long motelId, User currentUser);
    DashboardFilterResponse getFilters(User currentUser);
}
