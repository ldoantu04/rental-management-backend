package com.example.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDTO {
    private Long id;
    private String maMau;
    private String tenMau;
    private String tieuDe;
    private String noiDung;
    private Boolean batBuoc;
}
