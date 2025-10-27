package com.team14.chatbot.service;

import com.team14.chatbot.dto.request.ConversationRequest;
import com.team14.chatbot.dto.response.ConversationResponse;
import com.team14.chatbot.entity.Conversation;
import com.team14.chatbot.entity.User;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.exception.ErrorCode;
import com.team14.chatbot.mapper.ConversationMapper;
import com.team14.chatbot.repository.ConversationRepository;
import com.team14.chatbot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;
    ConversationMapper conversationMapper;
    UserRepository userRepository;

    public ConversationResponse create (ConversationRequest request) throws AppException {
        Conversation conversation = conversationMapper.toConversation(request);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        conversation.setCreatedDate(Instant.now());
        conversation.setUserId(user.getId());
        return conversationMapper.toConversationResponse(conversationRepository.save(conversation));
    }

    public List<ConversationResponse> findAll (){
        return conversationRepository.findAll().stream().map(conversationMapper::toConversationResponse).toList();
    }

    public void delete (String id){
        conversationRepository.deleteById(id);
    }

}
