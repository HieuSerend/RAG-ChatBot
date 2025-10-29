package com.team14.chatbot.service;

import com.team14.chatbot.dto.request.MessageRequest;
import com.team14.chatbot.dto.response.MessageResponse;
import com.team14.chatbot.entity.Conversation;
import com.team14.chatbot.entity.Message;
import com.team14.chatbot.entity.User;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.exception.ErrorCode;
import com.team14.chatbot.mapper.MessageMapper;
import com.team14.chatbot.repository.ConversationRepository;
import com.team14.chatbot.repository.MessageRepository;
import com.team14.chatbot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {
    MessageMapper messageMapper;
    MessageRepository messageRepository;
    ConversationRepository conversationRepository;
    UserRepository userRepository;

    public MessageResponse create (MessageRequest request) throws AppException {
        Conversation conversation = conversationRepository.findById(request.getConversationId()).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if(!conversation.getUserId().equals(user.getId())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        Message message = messageMapper.toMessage(request);
        message.setUserId(user.getId());
        return this.toMessageResponse(messageRepository.save(message));
    }



    public List<MessageResponse> findAll (String conversationId){
        return messageRepository.findAllByConversationId(conversationId)
                .stream().map(message -> {
                    try {
                        return toMessageResponse(message);
                    } catch (AppException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
    }

    private MessageResponse toMessageResponse (Message message) throws AppException {
        MessageResponse messageResponse = messageMapper.toMessageResponse(message);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (messageResponse.getUserId().equals(user.getId())){
            messageResponse.setMe(true);
        } else {
            messageResponse.setMe(false);
        }
        return messageResponse;
    }
}
