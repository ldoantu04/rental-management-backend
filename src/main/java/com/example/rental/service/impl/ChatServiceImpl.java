package com.example.rental.service.impl;

import com.example.rental.domain.ChatConversationStatus;
import com.example.rental.domain.ChatMessageRole;
import com.example.rental.model.ChatConversation;
import com.example.rental.model.ChatMessage;
import com.example.rental.model.User;
import com.example.rental.repository.ChatConversationRepository;
import com.example.rental.repository.ChatMessageRepository;
import com.example.rental.service.ChatService;
import com.example.rental.service.ai.PlannerService;
import com.example.rental.service.ai.dto.PlannerResult;
import com.example.rental.dto.ChatConversationResponse;
import com.example.rental.dto.ChatMessageResponse;
import com.example.rental.dto.ChatSendRequest;
import com.example.rental.dto.ChatSendResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final PlannerService plannerService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatSendResponse sendMessage(ChatSendRequest req, User user) throws Exception {
        if (req == null || req.getNoiDung() == null || req.getNoiDung().isBlank()) {
            throw new IllegalArgumentException("Noi dung tin nhan khong duoc de trong");
        }
        ChatConversation conversation = getOrCreateConversation(req.getHoiThoaiId(), user);
        saveMessage(conversation, ChatMessageRole.NGUOI_DUNG, req.getNoiDung(), null);

        PlannerResult result;
        try {
            result = plannerService.run(req.getNoiDung(), loadHistory(conversation), user);
        } catch (Exception e) {
            log.error("Loi khi goi AI: {}", e.getMessage(), e);
            result = PlannerResult.error("Xin loi, toi dang gap su co khi ket noi voi dich vu AI. Vui long thu lai sau.");
        }

        String text = result.getText() != null ? result.getText() : "...";
        ChatMessage assistantMessage = saveMessage(
                conversation,
                ChatMessageRole.TRO_LY,
                text,
                result.getTrace() == null ? null : objectMapper.valueToTree(result.getTrace()).toString());

        conversation.setNgaySua(LocalDateTime.now());
        if (conversation.getTieuDe() == null || conversation.getTieuDe().isBlank()
                || conversation.getTieuDe().equals("Cuoc tro chuyen moi")) {
            conversation.setTieuDe(generateTitle(req.getNoiDung()));
        }
        conversationRepository.save(conversation);

        List<ChatMessage> all = messageRepository.findByHoiThoaiIdOrderByNgayTaoAsc(conversation.getId());
        ChatMessage userMessage = all.size() >= 2 ? all.get(all.size() - 2) : all.get(0);
        ChatSendResponse response = new ChatSendResponse();
        response.setHoiThoaiId(conversation.getId());
        response.setTinNhanNguoiDung(toDto(userMessage));
        response.setTinNhanTroLy(toDto(assistantMessage));
        response.setToanBoTinNhan(toDtos(all));
        return response;
    }

    @Override
    public List<ChatConversationResponse> listConversations(User user) {
        List<ChatConversation> list = conversationRepository
                .findByNguoiDungIdAndTrangThaiOrderByNgaySuaDesc(user.getId(), ChatConversationStatus.DANG_HOAT_DONG);
        List<ChatConversationResponse> result = new ArrayList<>();
        for (ChatConversation c : list) {
            ChatConversationResponse dto = toConversationDto(c);
            List<ChatMessage> msgs = messageRepository.findByHoiThoaiIdOrderByNgayTaoAsc(c.getId());
            if (!msgs.isEmpty()) {
                ChatMessage last = msgs.get(msgs.size() - 1);
                String content = last.getNoiDung();
                dto.setTinNhanMoiNhat(content != null && content.length() > 80
                        ? content.substring(0, 80) + "..."
                        : content);
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<ChatMessageResponse> getConversationMessages(Long hoiThoaiId, User user) throws Exception {
        ChatConversation conversation = conversationRepository.findById(hoiThoaiId)
                .orElseThrow(() -> new Exception("Khong tim thay cuoc tro chuyen"));
        if (!conversation.getNguoiDung().getId().equals(user.getId())) {
            throw new Exception("Ban khong co quyen voi cuoc tro chuyen nay");
        }
        return toDtos(messageRepository.findByHoiThoaiIdOrderByNgayTaoAsc(hoiThoaiId));
    }

    @Override
    @Transactional
    public void deleteConversation(Long hoiThoaiId, User user) throws Exception {
        ChatConversation conversation = conversationRepository.findById(hoiThoaiId)
                .orElseThrow(() -> new Exception("Khong tim thay cuoc tro chuyen"));
        if (!conversation.getNguoiDung().getId().equals(user.getId())) {
            throw new Exception("Ban khong co quyen voi cuoc tro chuyen nay");
        }
        conversation.setTrangThai(ChatConversationStatus.DA_XOA);
        conversation.setNgaySua(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private List<PlannerService.ChatHistoryTurn> loadHistory(ChatConversation conversation) {
        List<ChatMessage> all = messageRepository.findByHoiThoaiIdOrderByNgayTaoAsc(conversation.getId());
        return all.stream()
                .filter(m -> m.getVaiTro() == ChatMessageRole.NGUOI_DUNG || m.getVaiTro() == ChatMessageRole.TRO_LY)
                .map(m -> new PlannerService.ChatHistoryTurn(
                        m.getVaiTro() == ChatMessageRole.NGUOI_DUNG
                                ? PlannerService.ChatHistoryTurn.Role.USER
                                : PlannerService.ChatHistoryTurn.Role.ASSISTANT,
                        m.getNoiDung()))
                .collect(Collectors.toList());
    }

    private ChatConversation getOrCreateConversation(Long hoiThoaiId, User user) {
        if (hoiThoaiId != null) {
            ChatConversation existing = conversationRepository.findById(hoiThoaiId).orElse(null);
            if (existing != null && existing.getNguoiDung().getId().equals(user.getId())
                    && existing.getTrangThai() == ChatConversationStatus.DANG_HOAT_DONG) {
                return existing;
            }
        }
        ChatConversation conversation = new ChatConversation();
        conversation.setNguoiDung(user);
        conversation.setTieuDe("Cuoc tro chuyen moi");
        conversation.setTrangThai(ChatConversationStatus.DANG_HOAT_DONG);
        conversation.setNgayTao(LocalDateTime.now());
        conversation.setNgaySua(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    private ChatMessage saveMessage(ChatConversation conversation, ChatMessageRole role, String content, String duLieuJson) {
        ChatMessage msg = new ChatMessage();
        msg.setHoiThoai(conversation);
        msg.setVaiTro(role);
        msg.setNoiDung(content);
        msg.setDuLieuJson(duLieuJson);
        msg.setNgayTao(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    private String generateTitle(String userInput) {
        if (userInput == null || userInput.isBlank()) return "Cuoc tro chuyen moi";
        String trimmed = userInput.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) + "..." : trimmed;
    }

    private ChatMessageResponse toDto(ChatMessage m) {
        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setId(m.getId());
        dto.setHoiThoaiId(m.getHoiThoai() != null ? m.getHoiThoai().getId() : null);
        dto.setVaiTro(m.getVaiTro());
        dto.setNoiDung(m.getNoiDung());
        dto.setDuLieuJson(m.getDuLieuJson());
        dto.setNgayTao(m.getNgayTao());
        return dto;
    }

    private List<ChatMessageResponse> toDtos(List<ChatMessage> messages) {
        List<ChatMessageResponse> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            result.add(toDto(m));
        }
        return result;
    }

    private ChatConversationResponse toConversationDto(ChatConversation c) {
        ChatConversationResponse dto = new ChatConversationResponse();
        dto.setId(c.getId());
        dto.setTieuDe(c.getTieuDe());
        dto.setNguoiDungId(c.getNguoiDung() != null ? c.getNguoiDung().getId() : null);
        dto.setTrangThai(c.getTrangThai());
        dto.setNgayTao(c.getNgayTao());
        dto.setNgaySua(c.getNgaySua());
        return dto;
    }
}
