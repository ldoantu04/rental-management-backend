package com.example.rental.repository;

import com.example.rental.model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByMaMau(String maMau);
    List<EmailTemplate> findAllByOrderByIdAsc();
    boolean existsByMaMau(String maMau);
}
