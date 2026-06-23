package com.example.rental.repository;

import com.example.rental.domain.ChatConversationStatus;
import com.example.rental.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByNguoiDungIdAndTrangThaiOrderByNgaySuaDesc(Long nguoiDungId, ChatConversationStatus trangThai);
    List<ChatConversation> findByNguoiDungIdOrderByNgaySuaDesc(Long nguoiDungId);
}
