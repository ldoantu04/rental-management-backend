package com.example.rental.controller;

import com.example.rental.domain.NotificationType;
import com.example.rental.dto.ApiResponse;
import com.example.rental.model.Notification;
import com.example.rental.model.User;
import com.example.rental.service.NotificationService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(required = false) NotificationType loai,
            @RequestParam(required = false) Boolean chuaDoc) throws Exception {
        User user = userService.findByJwt(jwt);
        notificationService.syncContractExpiryNotifications(user);
        notificationService.syncOverdueInvoiceNotifications(user);
        List<Notification> list;
        if (loai != null) {
            list = notificationService.findByUserAndType(user.getId(), loai);
        } else if (Boolean.TRUE.equals(chuaDoc)) {
            list = notificationService.findUnreadByUser(user.getId());
        } else {
            list = notificationService.findByUser(user.getId());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        long count = notificationService.countUnreadByUser(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long id) throws Exception {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        notificationService.markAllAsRead(user.getId());
        ApiResponse res = new ApiResponse();
        res.setMessage("Da danh dau tat ca thong bao la da doc");
        return ResponseEntity.ok(res);
    }
}
