package com.example.rental.service.impl;

import com.example.rental.domain.ChatConversationStatus;
import com.example.rental.domain.ChatMessageRole;
import com.example.rental.model.ChatConversation;
import com.example.rental.model.ChatMessage;
import com.example.rental.model.User;
import com.example.rental.repository.ChatConversationRepository;
import com.example.rental.repository.ChatMessageRepository;
import com.example.rental.service.ChatService;
import com.example.rental.service.ai.ChatActionService;
import com.example.rental.service.ai.PlannerService;
import com.example.rental.service.ai.dto.PlannerResult;
import com.example.rental.dto.ChatConfirmRequest;
import com.example.rental.dto.ChatConversationResponse;
import com.example.rental.dto.ChatMessageResponse;
import com.example.rental.dto.ChatSendRequest;
import com.example.rental.dto.ChatSendResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat orchestration. Delegates the actual reasoning loop to {@link PlannerService}.
 * This class is responsible for:
 *   - loading / persisting conversation history,
 *   - turning a {@link PlannerResult} into a stored assistant message,
 *   - dispatching confirmed mutations through {@link ChatActionService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatActionService actionService;
    private final PlannerService plannerService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatSendResponse sendMessage(ChatSendRequest req, User user) throws Exception {
        if (req == null || req.getNoiDung() == null || req.getNoiDung().isBlank()) {
            throw new IllegalArgumentException("Noi dung tin nhan khong duoc de trong");
        }
        ChatConversation conversation = getOrCreateConversation(req.getHoiThoaiId(), user);
        saveMessage(conversation, ChatMessageRole.NGUOI_DUNG, req.getNoiDung(), null, null, null, false, null);

        PlannerResult result;
        try {
            result = plannerService.run(req.getNoiDung(), loadHistory(conversation), user);
        } catch (Exception e) {
            log.error("Loi khi goi AI: {}", e.getMessage(), e);
            result = PlannerResult.error("Xin loi, toi dang gap su co ket noi voi dich vu AI. Vui long thu lai sau.");
        }

        ChatMessage assistantMessage;
        switch (result.getStatus()) {
            case NEEDS_CONFIRM -> {
                JsonNode payload = result.getPayload() == null ? objectMapper.createObjectNode() : result.getPayload();
                assistantMessage = saveMessage(
                        conversation,
                        ChatMessageRole.TRO_LY,
                        buildConfirmationText(result.getHanhDong(), result.getMoTaNgan()),
                        null,
                        result.getHanhDong(),
                        payload.toString(),
                        true,
                        null);
            }
            case NEEDS_INFO, ANSWER, ERROR -> {
                String text = result.getText() != null ? result.getText() : "...";
                assistantMessage = saveMessage(
                        conversation,
                        ChatMessageRole.TRO_LY,
                        text,
                        result.getTrace() == null ? null : objectMapper.valueToTree(result.getTrace()).toString(),
                        null,
                        null,
                        false,
                        null);
            }
            default -> {
                assistantMessage = saveMessage(
                        conversation,
                        ChatMessageRole.TRO_LY,
                        "Khong ro trang thai phan hoi",
                        null,
                        null,
                        null,
                        false,
                        null);
            }
        }

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
    @Transactional
    public ChatMessageResponse confirmAction(ChatConfirmRequest req, User user) throws Exception {
        if (req == null || req.getTinNhanId() == null) {
            throw new IllegalArgumentException("Thieu thong tin xac nhan");
        }
        ChatMessage assistantMsg = messageRepository.findById(req.getTinNhanId())
                .orElseThrow(() -> new Exception("Khong tim thay tin nhan"));

        ChatConversation conversation = assistantMsg.getHoiThoai();
        if (conversation == null || !conversation.getNguoiDung().getId().equals(user.getId())) {
            throw new Exception("Ban khong co quyen voi cuoc tro chuyen nay");
        }

        if (Boolean.TRUE.equals(assistantMsg.getDaXuLy())) {
            List<ChatMessage> all = messageRepository.findByHoiThoaiIdOrderByNgayTaoAsc(conversation.getId());
            if (!all.isEmpty()) {
                return toDto(all.get(all.size() - 1));
            }
        }

        if (Boolean.FALSE.equals(req.getXacNhan())) {
            assistantMsg.setDaXuLy(true);
            assistantMsg.setKetQuaHanhDong("Da huy thao tac");
            messageRepository.save(assistantMsg);

            ChatMessage reply = saveMessage(
                    conversation,
                    ChatMessageRole.TRO_LY,
                    "Da huy thao tac. Ban co the yeu cau thao tac khac.",
                    null,
                    null,
                    null,
                    false,
                    null);
            return toDto(reply);
        }

        try {
            Object result = actionService.execute(
                    assistantMsg.getHanhDongChoXacNhan(),
                    assistantMsg.getPayloadHanhDong(),
                    user);
            assistantMsg.setDaXuLy(true);
            String successText = "Thao tac da thuc hien thanh cong.\nChi tiet: " + describeResult(result);
            assistantMsg.setKetQuaHanhDong(successText);
            messageRepository.save(assistantMsg);

            ChatMessage reply = saveMessage(
                    conversation,
                    ChatMessageRole.TRO_LY,
                    successText + "\n\nBan can ho tro them gi khac?",
                    null,
                    null,
                    null,
                    false,
                    null);
            return toDto(reply);
        } catch (Exception ex) {
            log.warn("Loi thuc thi action AI: {}", ex.getMessage());
            assistantMsg.setDaXuLy(true);
            assistantMsg.setKetQuaHanhDong("That bai: " + ex.getMessage());
            messageRepository.save(assistantMsg);

            ChatMessage reply = saveMessage(
                    conversation,
                    ChatMessageRole.TRO_LY,
                    "Khong the thuc hien: " + ex.getMessage() + "\n\nVui long kiem tra lai thong tin va thu lai.",
                    null,
                    null,
                    null,
                    false,
                    null);
            return toDto(reply);
        }
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

    private String buildConfirmationText(String hanhDong, String moTa) {
        if (moTa == null || moTa.isBlank()) {
            return "Toi se thuc hien thao tac: " + hanhDong + "\n\nBan co xac nhan thuc hien khong?";
        }
        return "Toi se thuc hien: " + moTa + "\n\nBan co xac nhan thuc hien khong? (tra loi 'Co' hoac 'Dong y' de xac nhan, hoac 'Huy' de huy bo)";
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

    private ChatMessage saveMessage(ChatConversation conversation, ChatMessageRole role, String content,
                                    String duLieuJson, String hanhDong, String payloadHanhDong,
                                    boolean canXacNhan, String ketQua) {
        ChatMessage msg = new ChatMessage();
        msg.setHoiThoai(conversation);
        msg.setVaiTro(role);
        msg.setNoiDung(content);
        msg.setDuLieuJson(duLieuJson);
        msg.setHanhDongChoXacNhan(hanhDong);
        msg.setPayloadHanhDong(payloadHanhDong);
        msg.setCanXacNhan(canXacNhan);
        msg.setDaXuLy(false);
        msg.setKetQuaHanhDong(ketQua);
        msg.setNgayTao(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    private String describeResult(Object result) {
        if (result == null) return "khong co";
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (result instanceof com.example.rental.model.Room r) {
                node.put("id", r.getId());
                node.put("maPhong", r.getMaPhong());
                if (r.getNhaTro() != null) node.put("tenTro", r.getNhaTro().getTenTro());
                node.put("trangThai", r.getTrangThai() != null ? r.getTrangThai().name() : null);
            } else if (result instanceof com.example.rental.model.Tenant t) {
                node.put("id", t.getId());
                node.put("hoTen", t.getHoTen());
            } else if (result instanceof com.example.rental.model.Contract c) {
                node.put("id", c.getId());
                node.put("maHopDong", c.getMaHopDong());
            } else if (result instanceof com.example.rental.model.Invoice i) {
                node.put("id", i.getId());
                node.put("maHoaDon", i.getMaHoaDon());
            } else if (result instanceof com.example.rental.model.Motel m) {
                node.put("id", m.getId());
                node.put("tenTro", m.getTenTro());
            } else {
                return result.toString();
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return result.toString();
        }
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
        dto.setCanXacNhan(m.getCanXacNhan());
        dto.setHanhDongChoXacNhan(m.getHanhDongChoXacNhan());
        dto.setPayloadHanhDong(m.getPayloadHanhDong());
        dto.setDaXuLy(m.getDaXuLy());
        dto.setKetQuaHanhDong(m.getKetQuaHanhDong());
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
