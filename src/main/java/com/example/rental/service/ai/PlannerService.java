package com.example.rental.service.ai;

import com.example.rental.domain.UserRole;
import com.example.rental.model.User;
import com.example.rental.service.ai.dto.PlannerResult;
import com.example.rental.service.ai.dto.PlannerStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlannerService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final BusinessToolService businessToolService;

    private static final Pattern STEP_JSON = Pattern.compile(
            "```json\\s*(\\{[\\s\\S]*?\"businessTool\"[\\s\\S]*?\\})\\s*```",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_JSON = Pattern.compile(
            "^\\s*\\{[\\s\\S]*\"businessTool\"[\\s\\S]*\\}\\s*$");

    private static final int MAX_STEPS = 6;

    private static final String PLANNER_SYSTEM_PROMPT = """
            Ban la **SmartRental AI**, tro ly tra cuu thong tin cho he thong quan ly nha tro.
            Ban chi tra cuu thong tin, KHONG thuc hien bat ky thao tac tao/sua/xoa nao.

            # Nhung gi ban CO THE tra cuu
            - Tong quan he thong (so nha tro, phong, khach thue, hop dong, hoa don, doanh thu)
            - Thong tin nha tro (danh sach, dia chi, so phong, trang thai)
            - Thong tin phong tro (ma phong, gia thue, trang thai, dien tich, so nguoi, nha tro chua)
            - Thong tin khach thue (ho ten, SDT, CCCD, phong dang thue, hop dong)
            - Thong tin hop dong (ma hop dong, ngay bat dau/ket thuc, trang thai, gia thue, tien coc)
            - Thong tin hoa don (ky hoa don, tong tien, trang thai thanh toan, chi so dien/nuoc)
            - Doanh thu theo thang/nam
            - Phong dang trong, phong sap het han hop dong, hoa don chua thanh toan
            - **Thong tin nhan vien / tai khoan** (chi danh cho QUAN LY): ho ten, email, SDT, vai tro, nha tro duoc phan cong

            # Business Tool tra cuu (chỉ dùng tool này, KHÔNG gọi tool tạo/sửa/xóa)
            - getRoomOverview(maPhong?, tenNhaTro?)
            - getTenantOverview(hoTen?, sdt?, cccd?)
            - getRoomOfTenant(hoTen?, sdt?, cccd?)
            - getActiveTenants(tenNhaTro?)
            - getContractsOfRoom(maPhong, tenNhaTro?)
            - checkRoomStatus(maPhong, tenNhaTro?)
            - getInvoiceOverview(maHoaDon?, maPhong?, tenNhaTro?)
            - calculateInvoice(maPhong?, tenNhaTro?, maHopDong?)

            # Quy tắc bắt buộc
            - KHÔNG hỏi về ID nội bộ (roomId, contractId...). Hệ thống tự resolve từ tên/mã.
            - KHÔNG bịa số liệu. Nếu cần dữ liệu, gọi Business Tool tra cứu.
            - KHÔNG đề cập đến ID database, internal fields, hay cấu trúc bảng.
            - Nếu không tìm thấy dữ liệu, thông báo rõ ràng bằng tiếng Việt.
            - Luôn phản hồi bằng tiếng Việt, thân thiện, chuyên nghiệp.
            - KHÔNG gợi ý hay đề xuất các thao tác tạo/sửa/xóa dữ liệu.

            # Phân quyền
            - Nhân viên: chỉ truy cập nha tro/phòng được phân công quản lý.
            - Quản lý: truy cập toàn bộ hệ thống.

            # Quy tắc thời gian
            Backend lưu trữ ngày dạng ISO `yyyy-MM-dd`. Khi user nói tiếng Việt, chủ động quy đổi:
            - "tháng này" -> tháng hiện tại của năm hiện tại.
            - "tháng sau" -> tháng kế tiếp.
            - "tháng trước" -> tháng trước đó.
            - "tháng 7" -> `2026-07` (năm hiện tại).
            - "năm nay" -> năm hiện tại.
            - "hôm nay" -> ngày hôm nay.
            KHÔNG ĐƯỢC hardcode năm.

            # Format phản hồi
            Khi gọi Business Tool, trả về JSON:
            ```json
            {
              "intent": "Tra cuu thong tin phong",
              "businessTool": "getRoomOverview",
              "args": { "maPhong": "P203" },
              "thought": "Can xem phong P203"
            }
            ```

            Khi đã có đủ thông tin, trả về câu trả lời bằng tiếng Việt (không bọc ```json).
            """;

    public PlannerResult run(String userInput, List<ChatHistoryTurn> history, User user) {
        List<Message> messages = buildMessages(history, user);
        PlannerResult result = new PlannerResult();
        for (int step = 0; step < MAX_STEPS; step++) {
            String llmReply;
            try {
                llmReply = chatClient.prompt()
                        .messages(messages)
                        .user(step == 0 ? userInput : "(tiep tuc)")
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("Loi khi goi AI: {}", e.getMessage());
                return PlannerResult.error("Xin loi, toi dang gap su co khi ket noi voi dich vu AI. Vui long thu lai sau.");
            }
            messages.add(new AssistantMessage(llmReply == null ? "" : llmReply));

            PlannerStep stepObj = tryParseStep(llmReply);
            if (stepObj != null) {
                result.addTrace("step." + step + ".planned", stepObj);
                Object toolResult;
                try {
                    toolResult = invokeBusinessTool(stepObj, user);
                } catch (ResolverService.AmbiguousMatchException amb) {
                    return PlannerResult.needsInfo(amb.getMessage());
                } catch (Exception ex) {
                    return PlannerResult.needsInfo("Khong the tra cuu: " + ex.getMessage());
                }
                result.addTrace("step." + step + ".result", toolResult);
                String observation = "Ket qua tu " + stepObj.getBusinessTool() + ": " + objectMapper.valueToTree(toolResult).toString();
                messages.add(new UserMessage(observation));
                continue;
            }

            String text = extractAnswerText(llmReply);
            result.addTrace("step." + step + ".answer", text);
            return PlannerResult.answer(text);
        }
        return PlannerResult.answer("Toi da tra cuu nhung chua co ket qua cuoi. Vui long mo ta lai yeu cau.");
    }

    private List<Message> buildMessages(List<ChatHistoryTurn> history, User user) {
        List<Message> messages = new ArrayList<>();
        String sys = PLANNER_SYSTEM_PROMPT;
        if (user != null && user.getVaiTro() == UserRole.NHAN_VIEN) {
            sys += "\n\nNguoi dung hien tai la NHAN VIEN. Chi truy cap duoc cac nha tro duoc phan cong.";
        }
        messages.add(new SystemMessage(sys));
        if (history != null) {
            for (ChatHistoryTurn t : history) {
                if (t.role() == ChatHistoryTurn.Role.USER) {
                    messages.add(new UserMessage(t.content() == null ? "" : t.content()));
                } else {
                    messages.add(new AssistantMessage(t.content() == null ? "" : t.content()));
                }
            }
        }
        return messages;
    }

    private PlannerStep tryParseStep(String text) {
        if (text == null) return null;
        Matcher m = STEP_JSON.matcher(text);
        if (!m.find()) return null;
        try {
            JsonNode node = objectMapper.readTree(m.group(1));
            PlannerStep step = new PlannerStep();
            step.setIntent(node.path("intent").asText(null));
            step.setBusinessTool(node.path("businessTool").asText(null));
            step.setThought(node.path("thought").asText(null));
            JsonNode args = node.path("args");
            step.setArgs(args.isObject() ? args : objectMapper.createObjectNode());
            if (step.getBusinessTool() == null || step.getBusinessTool().isBlank()) return null;
            return step;
        } catch (Exception e) {
            log.warn("Khong parse duoc step JSON: {}", e.getMessage());
            return null;
        }
    }

    private String extractAnswerText(String text) {
        if (text == null) return "Xin loi, toi chua the tra loi.";
        String stripped = text.replaceAll("```json[\\s\\S]*?```", "").trim();
        return stripped.isEmpty() ? text.trim() : stripped;
    }

    private Object invokeBusinessTool(PlannerStep step, User user) throws Exception {
        String tool = step.getBusinessTool();
        JsonNode args = step.getArgs() == null ? objectMapper.createObjectNode() : step.getArgs();
        Method m = findToolMethod(tool);
        if (m == null) {
            throw new IllegalArgumentException("Business Tool khong ton tai: " + tool);
        }
        return m.invoke(businessToolService, args, user);
    }

    private Method findToolMethod(String toolName) {
        if (toolName == null) return null;
        String target = toolName.trim();
        for (Method m : BusinessToolService.class.getMethods()) {
            if (m.getName().equalsIgnoreCase(target)
                    && m.getParameterCount() == 2
                    && m.getParameterTypes()[0].equals(JsonNode.class)
                    && m.getParameterTypes()[1].equals(User.class)) {
                return m;
            }
        }
        return null;
    }

    public record ChatHistoryTurn(Role role, String content) {
        public enum Role { USER, ASSISTANT }
    }
}
