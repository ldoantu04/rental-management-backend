package com.example.rental.dto;

import lombok.Data;

@Data
public class RoomStatusResponse {
    private int rented;
    private int vacant;
    private int maintenance;
    private int total;
}
