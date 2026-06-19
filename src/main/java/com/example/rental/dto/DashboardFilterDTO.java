package com.example.rental.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardFilterDTO {
    private List<Integer> years;
    private List<MotelOption> motels;

    @Data
    public static class MotelOption {
        private Long id;
        private String name;

        public MotelOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
