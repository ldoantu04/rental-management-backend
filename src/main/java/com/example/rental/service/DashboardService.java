package com.example.rental.service;

import com.example.rental.dto.DashboardFilterDTO;
import com.example.rental.dto.RevenueChartDTO;
import com.example.rental.dto.RoomStatusDTO;
import com.example.rental.model.User;

import java.util.List;

public interface DashboardService {
    List<RevenueChartDTO> getRevenueChart(int year, Long motelId, User currentUser);
    RoomStatusDTO getRoomStatus(Long motelId, User currentUser);
    DashboardFilterDTO getFilters(User currentUser);
}
