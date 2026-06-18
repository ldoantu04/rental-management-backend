package com.example.rental.controller;

import com.example.rental.config.VNPayConfig;
import com.example.rental.dto.ApiResponse;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final VNPayConfig vnPayConfig;

    @GetMapping("/pay/{maHoaDon}")
    public void startPaymentFromEmail(@PathVariable String maHoaDon, HttpServletRequest request, HttpServletResponse response) throws IOException {
        processPaymentRedirect(maHoaDon, request, response);
    }

    @GetMapping("/payment/{maHoaDon}")
    public void startPaymentFromPortal(@PathVariable String maHoaDon, HttpServletRequest request, HttpServletResponse response) throws IOException {
        processPaymentRedirect(maHoaDon, request, response);
    }

    @PostMapping(value = "/api/payment/create/{maHoaDon}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPaymentApi(@PathVariable String maHoaDon, HttpServletRequest request) {
        return processPaymentApi(maHoaDon, request);
    }

    @GetMapping(value = "/api/payment/{maHoaDon}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPaymentUrl(@PathVariable String maHoaDon, HttpServletRequest request) {
        return processPaymentApi(maHoaDon, request);
    }

    @GetMapping("/payment/return")
    public ResponseEntity<?> paymentReturn(@RequestParam Map<String, String> params) {
        try {
            paymentService.processReturn(params);
            return ResponseEntity.ok(buildReturnResponse(params, true));
        } catch (Exception e) {
            log.error("Loi xu ly return URL: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(buildReturnResponse(params, false));
        }
    }

    @PostMapping(value = "/api/payment/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> paymentIpn(@RequestParam Map<String, String> params) {
        Map<String, String> response = new LinkedHashMap<>();
        try {
            if (!paymentService.verifySignature(params)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return ResponseEntity.ok(response);
            }
            String vnpTxnRef = params.get("vnp_TxnRef");
            if (vnpTxnRef == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return ResponseEntity.ok(response);
            }

            String vnpResponseCode = params.get("vnp_ResponseCode");
            if (!"00".equals(vnpResponseCode)) {
                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
                return ResponseEntity.ok(response);
            }

            try {
                paymentService.processIpn(params);
            } catch (Exception ex) {
                log.error("Loi xu ly IPN: {}", ex.getMessage(), ex);
                response.put("RspCode", "99");
                response.put("Message", "Unknown error");
                return ResponseEntity.ok(response);
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Loi tiep nhan IPN: {}", e.getMessage(), e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return ResponseEntity.ok(response);
        }
    }

    private void processPaymentRedirect(String maHoaDon, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PaymentService.PaymentResult result = paymentService.createPayment(maHoaDon, request);
            if (!"00".equals(result.getCode())) {
                writeErrorPage(response, result.getMessage(), maHoaDon, 400);
                return;
            }
            response.setStatus(302);
            response.setHeader("Location", result.getPaymentUrl());
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.flushBuffer();
        } catch (Exception e) {
            log.error("Loi khoi tao thanh toan: {}", e.getMessage(), e);
            writeErrorPage(response, "Loi he thong: " + e.getMessage(), maHoaDon, 500);
        }
    }

    private ResponseEntity<?> processPaymentApi(String maHoaDon, HttpServletRequest request) {
        try {
            PaymentService.PaymentResult result = paymentService.createPayment(maHoaDon, request);
            if (!"00".equals(result.getCode())) {
                ApiResponse res = new ApiResponse();
                res.setMessage(result.getMessage());
                return ResponseEntity.badRequest().body(res);
            }
            return ResponseEntity.ok(buildUrlResponse(result.getPaymentUrl(), maHoaDon));
        } catch (Exception e) {
            log.error("Loi khoi tao thanh toan: {}", e.getMessage(), e);
            ApiResponse res = new ApiResponse();
            res.setMessage("Loi khoi tao thanh toan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    private void writeErrorPage(HttpServletResponse response, String message, String maHoaDon, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("text/html; charset=UTF-8");
        String html = buildErrorPage(message, maHoaDon);
        response.getWriter().write(html);
        response.getWriter().flush();
    }

    private Map<String, Object> buildUrlResponse(String paymentUrl, String maHoaDon) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "00");
        body.put("message", "success");
        body.put("paymentUrl", paymentUrl);
        body.put("maHoaDon", maHoaDon);
        return body;
    }

    private Map<String, Object> buildReturnResponse(Map<String, String> params, boolean success) {
        Map<String, Object> body = new HashMap<>();
        body.put("vnp_TxnRef", params.get("vnp_TxnRef"));
        body.put("vnp_Amount", params.get("vnp_Amount"));
        body.put("vnp_OrderInfo", params.get("vnp_OrderInfo"));
        body.put("vnp_ResponseCode", params.get("vnp_ResponseCode"));
        body.put("vnp_TransactionNo", params.get("vnp_TransactionNo"));
        body.put("vnp_BankCode", params.get("vnp_BankCode"));
        body.put("vnp_PayDate", params.get("vnp_PayDate"));
        body.put("vnp_TransactionStatus", params.get("vnp_TransactionStatus"));
        body.put("success", success);
        return body;
    }

    private String buildErrorPage(String message, String maHoaDon) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Thanh toan</title>"
                + "<style>body{font-family:Arial,sans-serif;background:#F6F7FB;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}"
                + ".card{background:#fff;max-width:480px;width:100%;padding:32px;border-radius:16px;box-shadow:0 10px 25px rgba(0,0,0,.08);text-align:center}"
                + ".badge{display:inline-block;padding:6px 14px;border-radius:999px;background:#FEE2E2;color:#B91C1C;font-size:12px;font-weight:600;margin-bottom:16px}"
                + "h1{color:#111827;font-size:22px;margin:0 0 8px}"
                + "p{color:#6B7280;line-height:1.6;margin:0 0 12px}"
                + ".code{display:inline-block;background:#F3F4F6;padding:6px 12px;border-radius:8px;font-size:12px;color:#374151}"
                + "</style></head><body><div class='card'>"
                + "<div class='badge'>Khong the thanh toan</div>"
                + "<h1>" + escape(message) + "</h1>"
                + "<p>Vui long lien he ban quan ly neu can ho tro them.</p>"
                + "<p class='code'>Ma hoa don: " + escape(maHoaDon) + "</p>"
                + "</div></body></html>";
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
