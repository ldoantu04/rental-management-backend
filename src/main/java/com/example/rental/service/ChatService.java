package com.example.rental.service;

import com.example.rental.dto.ChatConfirmRequest;
import com.example.rental.dto.ChatConversationDto;
import com.example.rental.dto.ChatMessageDto;
import com.example.rental.dto.ChatSendRequest;
import com.example.rental.dto.ChatSendResponse;
import com.example.rental.model.User;

import java.util.List;

public interface ChatService {

    ChatSendResponse sendMessage(ChatSendRequest req, User user) throws Exception;

    ChatMessageDto confirmAction(ChatConfirmRequest req, User user) throws Exception;

    List<ChatConversationDto> listConversations(User user);

    List<ChatMessageDto> getConversationMessages(Long hoiThoaiId, User user) throws Exception;

    void deleteConversation(Long hoiThoaiId, User user) throws Exception;
}
