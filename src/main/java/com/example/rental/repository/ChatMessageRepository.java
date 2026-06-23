package com.example.rental.repository;

import com.example.rental.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByHoiThoaiIdOrderByNgayTaoAsc(Long hoiThoaiId);
}
