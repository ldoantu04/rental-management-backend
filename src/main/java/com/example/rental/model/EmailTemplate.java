package com.example.rental.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_template")
@Data
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String maMau;

    @Column(nullable = false)
    private String tenMau;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tieuDe;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Column(nullable = false)
    private Boolean batBuoc = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime ngayTao;

    @UpdateTimestamp
    private LocalDateTime ngaySua;
}
