package com.example.rental.service;

import com.example.rental.dto.ChatConversationResponse;
import com.example.rental.dto.ChatMessageResponse;
import com.example.rental.dto.ChatSendRequest;
import com.example.rental.dto.ChatSendResponse;
import com.example.rental.model.User;

import java.util.List;

public interface ChatService {

    ChatSendResponse sendMessage(ChatSendRequest req, User user) throws Exception;

    List<ChatConversationResponse> listConversations(User user);

    List<ChatMessageResponse> getConversationMessages(Long hoiThoaiId, User user) throws Exception;

    void deleteConversation(Long hoiThoaiId, User user) throws Exception;
}
