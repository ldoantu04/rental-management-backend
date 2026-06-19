package com.example.rental.service.ai;

import com.example.rental.domain.UserRole;
import com.example.rental.model.User;
import com.example.rental.service.ai.dto.PlannerResult;
import com.example.rental.service.ai.dto.PlannerStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Planner Agent: orchestrates a multi-step workflow between the LLM and the
 * Business Layer. The LLM is only allowed to emit one of two things per turn:
 *   1. a JSON object describing the next business step
 *      ({@code {"intent": "...", "businessTool": "...", "args": {...}, "thought": "..."}}),
 *   2. or a natural-language answer for the user.
 *
 * The Planner parses step #1, invokes the matching method on
 * {@link BusinessToolService}, feeds the structured result back to the LLM,
 * and loops until the LLM returns a final natural-language answer.
 *
 * Mutation Business Tools (anything that changes data) are NOT executed here.
 * They are exposed as a single "needs_confirm" JSON the frontend can show a
 * confirmation dialog for. Actual execution happens via the existing
 * {@link ChatActionService#execute} path on the confirm endpoint.
 */
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
    private static final Pattern CONFIRM_JSON = Pattern.compile(
            "```json\\s*(\\{[\\s\\S]*?\"hanhDong\"[\\s\\S]*?\\})\\s*```",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_JSON = Pattern.compile(
            "^\\s*\\{[\\s\\S]*\"(businessTool|hanhDong)\"[\\s\\S]*\\}\\s*$");

    private static final int MAX_STEPS = 6;

    private static final String PLANNER_SYSTEM_PROMPT = """
            Bạn là **SmartRental AI Planner**, bộ điều phối trung tâm của hệ thống quản lý nhà trọ.
            Bạn KHÔNG truy vấn database trực tiếp và KHÔNG bịa số. Mọi dữ liệu thật phải đến từ Business Tool.

            # Quy trình bắt buộc mỗi lượt
            1. Phân tích yêu cầu người dùng.
            2. Xác định dữ liệu cần có.
            3. Tự quyết định Business Tool cần gọi tiếp theo (hoặc cho câu trả lời cuối).
            4. Trả về ĐÚNG MỘT khối JSON trong ```json ...```.

            # Business Tool có sẵn (chỉ gọi đúng tên này)

            ## Tra cứu / tổng quan (read-only, tự động thực thi)
            - getRoomOverview(maPhong?, tenNhaTro?)
            - getTenantOverview(hoTen?, sdt?, cccd?)
            - getRoomOfTenant(hoTen?, sdt?, cccd?)
            - getActiveTenants(tenNhaTro?)
            - getContractsOfRoom(maPhong, tenNhaTro?)
            - checkRoomStatus(maPhong, tenNhaTro?)
            - getInvoiceOverview(maHoaDon? hoặc maPhong? tenNhaTro?)
            - calculateInvoice(maPhong?, tenNhaTro?, maHopDong?, chiSoDienMoi?, chiSoNuocMoi?, giaDien?, giaNuoc?, kieuTinhNuoc?, tienPhong?)

            ## Tạo / sửa (CẦN user xác nhận, chỉ trả JSON hanhDong)
            - createInvoiceForRoom / createInvoiceByRoom(maPhong, tenNhaTro?, chiSoDienMoi, chiSoNuocMoi?, giaDien?, giaNuoc?, kieuTinhNuoc?, tienPhong?, hanThanhToan?, kyHoaDon?, ghiChu?)
            - createContractForRoom / assignTenantToRoom(maPhong, tenNhaTro?, hoTen?, sdt?, cccd?, ngayBatDau?, ngayKetThuc?, soThang?, giaThue?, tienCoc?)
            - renewContract / extendContract(maHopDong, ngayKetThuc?, soThang?)
            - terminateContract / closeContract(maHopDong, lyDo?)
            - checkoutTenant(maPhong?, tenNhaTro?, maHopDong?, lyDo?)
            - moveTenant(maHopDong, maPhongMoi, tenNhaTroMoi?)
            - updateInvoice(maHoaDon, chiSoDienMoi?, chiSoNuocMoi?, giaDien?, giaNuoc?, tienPhong?, hanThanhToan?, kyHoaDon?, ghiChu?)
            - deleteInvoice(maHoaDon)
            - collectPayment / createTransactionForInvoice(maHoaDon)
            - createMotel(tenTro, diaChi?, soTang?, tongPhong?, ghiChu?)   // chỉ Quản lý
            - createRoom(maPhong, tenNhaTro?, giaThue, dienTich?, soNguoi?, tang?, ghiChu?)
            - createTenant(hoTen, ngaySinh?, gioiTinh?, cccd?, sdt?, email?, diaChi?, ghiChu?)

            # Quy tắc vàng

            - KHÔNG hỏi user về ID nội bộ (roomId, contractId, tenantId, motelId, invoiceId). Hệ thống tự resolve từ tên/mã.
            - KHÔNG bịa số liệu. Nếu cần dữ liệu, gọi Business Tool tra cứu.
            - Với thao tác CREATE / UPDATE / DELETE: gọi read-only tool trước để xác minh, sau đó trả JSON hanhDong để hệ thống xin xác nhận.
            - Nếu nhiều kết quả trùng tên, gọi Business Tool tra cứu; nếu vẫn không phân biệt được thì hỏi user (text thuần).
            - Luôn phản hồi bằng tiếng Việt.
            - KHÔNG bao giờ tự ý thực thi thao tác thay đổi dữ liệu.

            # QUY TẮC RESOLVE ID (BẮT BUỘC)

            Ưu tiên TUYỆT ĐỐI theo thứ tự:

            1. Nếu người dùng nói "phòng P203, nhà trọ AB" -> gọi `getRoomOverview({maPhong:"P203", tenNhaTro:"AB"})` hoặc `checkRoomStatus` để resolve roomId + contractId + tenantId.
            2. Sau đó mới gọi business tool tạo/sửa với maPhong / tenNhaTro. KHÔNG cần truyền maHopDong vì business tool tự resolve.
            3. CHỈ truyền maHopDong khi người dùng đã cung cấp mã hợp đồng cụ thể (vd "hợp đồng HD001").
            4. CHỈ truyền maHoaDon khi người dùng cung cấp mã hóa đơn cụ thể.

            Khi cần confirm mutation, payload JSON KHÔNG ĐƯỢC chứa `maHopDong` mơ hồ / `null`. Các handler backend chỉ chấp nhận:

            - CREATE_INVOICE: cần `maHopDong` thật (long) HOẶC `maPhong` + `tenNhaTro` (để hệ thống tự resolve). Phải tự gọi read-only tool trước.
            - CREATE_CONTRACT: cần `maPhong` + `maKhachThue` (đều là long) HOẶC `maPhong` + `tenNhaTro` + thông tin khách (hoTen/sdt/cccd).
            - CREATE_ROOM: cần `maPhong` + `maNhaTro` (long) HOẶC `maPhong` + `tenNhaTro`.
            - UPDATE_INVOICE, DELETE_INVOICE: cần `maHoaDon` (string, có thể fuzzy match).
            - Các action khác: tuân theo schema đã liệt kê.

            Nếu sau khi tra cứu mà KHÔNG xác định được ID cần thiết, KHÔNG ĐƯỢC tự bịa số. Hãy hỏi user (text thuần) để lấy thêm thông tin.

            # QUY TẮC THỜI GIAN (BẮT BUỘC)

            Backend lưu trữ tất cả ngày tháng dưới dạng LocalDate (chuẩn ISO `yyyy-MM-dd`).
            Khi truyền ngày trong args của Business Tool hoặc payload JSON, BẮT BUỘC dùng:

            - Ngày đầy đủ: `yyyy-MM-dd` (ví dụ `2026-07-15`).
            - Kỳ hóa đơn: `yyyy-MM` (ví dụ `2026-07`) -- hệ thống tự hiểu là ngày 1 của tháng.
            - TUYỆT ĐỐI KHÔNG gửi `dd/MM/yyyy`, `MM/yyyy` hay bất kỳ format nào khác.

            Khi user nói các cụm tiếng Việt, hãy CHỦ ĐỘNG quy đổi sang ISO trước khi gọi tool:

            - "tháng này", "tháng hiện tại" -> tháng hiện tại của năm hiện tại.
            - "tháng sau" -> tháng kế tiếp.
            - "tháng trước" -> tháng trước đó.
            - "tháng 7", "tháng 7 năm nay" -> `2026-07` (năm hiện tại). Nếu user nói "tháng 7/2024" -> `2024-07`.
            - "năm nay" -> năm hiện tại (LocalDate.now()).
            - "quý này" -> tháng đầu quý hiện tại.
            - "hôm nay" -> ngày hôm nay.

            Khi nói riêng về hợp đồng với "tháng 12 tháng" (soThang=12): cộng trực tiếp vào ngày bắt đầu, không cần tính năm.

            KHÔNG ĐƯỢC hardcode năm. Luôn lấy năm hiện tại từ `LocalDate.now()` tại thời điểm xử lý.

            # Format phản hồi

            ## Bước tiếp theo (gọi Business Tool)
            ```json
            {
              "intent": "Tra cuu thong tin phong",
              "businessTool": "getRoomOverview",
              "args": { "maPhong": "P203" },
              "thought": "Can xem phong P203 dang co khach nao, hop dong gi"
            }
            ```

            ## Cần xác nhận thao tác
            ```json
            {
              "hanhDong": "CREATE_INVOICE",
              "moTaNgan": "Tao hoa don cho phong P203 ky 06/2026",
              "payload": { "maHopDong": 17, "chiSoDienMoi": 250, "chiSoNuocMoi": 88 }
            }
            ```

            ## Câu trả lời cuối cùng cho user
            Trả về văn bản thường (không bọc ```json) với phân tích ngắn gọn, chuyên nghiệp, tiếng Việt.

            Khi đã đủ thông tin để tạo hóa đơn / hợp đồng / thu tiền, LUÔN LUÔN trả JSON hanhDong để hệ thống xác nhận. Tuyệt đối KHÔNG tự thực thi.
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
                return PlannerResult.error("Xin loi, toi dang gap su co ket noi voi dich vu AI. Vui long thu lai sau.");
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
                    return PlannerResult.needsInfo("Khong the thuc hien: " + ex.getMessage());
                }
                result.addTrace("step." + step + ".result", toolResult);
                String observation = "Ket qua tu " + stepObj.getBusinessTool() + ": " + objectMapper.valueToTree(toolResult).toString();
                messages.add(new UserMessage(observation));
                continue;
            }

            JsonNode confirmNode = tryParseConfirm(llmReply);
            if (confirmNode != null) {
                result.addTrace("step." + step + ".confirm", confirmNode);
                String hanhDong = confirmNode.path("hanhDong").asText(null);
                String moTa = confirmNode.path("moTaNgan").asText(null);
                JsonNode payload = confirmNode.path("payload");
                if (hanhDong == null || hanhDong.isBlank()) {
                    return PlannerResult.needsInfo("AI yeu cau xac nhan nhung thieu hanhDong");
                }
                return PlannerResult.needsConfirm(hanhDong, moTa, payload);
            }

            String text = extractAnswerText(llmReply);
            result.addTrace("step." + step + ".answer", text);
            return PlannerResult.answer(text);
        }
        return PlannerResult.answer("Toi da hoan thanh cac buoc kiem tra nhung chua co cau tra loi cuoi. Vui long mo ta lai yeu cau.");
    }

    private List<Message> buildMessages(List<ChatHistoryTurn> history, User user) {
        List<Message> messages = new ArrayList<>();
        String sys = PLANNER_SYSTEM_PROMPT;
        if (user != null && user.getVaiTro() == UserRole.NHAN_VIEN) {
            sys += "\n\nNguoi dung hien tai la NHAN VIEN. Chi truy cap duoc cac nha tro duoc phan cong. Khong cung cap thong ke tong quan toan he thong.";
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

    private JsonNode tryParseConfirm(String text) {
        if (text == null) return null;
        Matcher m = CONFIRM_JSON.matcher(text);
        if (!m.find()) return null;
        try {
            JsonNode node = objectMapper.readTree(m.group(1));
            if (!node.hasNonNull("hanhDong")) return null;
            return node;
        } catch (Exception e) {
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
