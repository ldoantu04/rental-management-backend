package com.example.rental.controller;

import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.ChatConfirmRequest;
import com.example.rental.dto.ChatConversationDto;
import com.example.rental.dto.ChatMessageDto;
import com.example.rental.dto.ChatSendRequest;
import com.example.rental.dto.ChatSendResponse;
import com.example.rental.model.User;
import com.example.rental.service.ChatService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @PostMapping("/send")
    public ResponseEntity<ChatSendResponse> sendMessage(
            @RequestBody ChatSendRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        ChatSendResponse response = chatService.sendMessage(req, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ChatMessageDto> confirmAction(
            @RequestBody ChatConfirmRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        ChatMessageDto response = chatService.confirmAction(req, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationDto>> listConversations(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        return ResponseEntity.ok(chatService.listConversations(user));
    }

    @GetMapping("/conversations/{hoiThoaiId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long hoiThoaiId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        return ResponseEntity.ok(chatService.getConversationMessages(hoiThoaiId, user));
    }

    @DeleteMapping("/conversations/{hoiThoaiId}")
    public ResponseEntity<ApiResponse> deleteConversation(
            @PathVariable Long hoiThoaiId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        chatService.deleteConversation(hoiThoaiId, user);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa cuoc tro chuyen thanh cong");
        return ResponseEntity.ok(res);
    }
}
