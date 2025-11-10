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
import com.team14.chatbot.repository.HybridChatMemoryRepository;
import com.team14.chatbot.repository.MessageRepository;
import com.team14.chatbot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {
    MessageMapper messageMapper;
    MessageRepository messageRepository;
    ConversationRepository conversationRepository;
    UserRepository userRepository;
    ChatService chatService;
    HybridChatMemoryRepository hybridChatMemoryRepository;
    ChatClient chatClient;

    public Flux<String> streamingCreate (MessageRequest request) throws AppException {
        Conversation conversation = conversationRepository.findById(request.getConversationId()).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if(!conversation.getUserId().equals(user.getId())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        UserMessage userQuery = new UserMessage(request.getText());
        Prompt prompt = chatService.generatePrompt(userQuery, request.getConversationId());

        StringBuilder aiResponse = new StringBuilder();

        return chatClient.prompt(prompt)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    aiResponse.append(chunk);

                })
                .doOnComplete(() -> {
                    Message userMessage = Message.builder()
                            .text(request.getText())
                            .conversationId(request.getConversationId())
                            .role(MessageType.USER.name())
                            .build();

                    Message aiMessage = Message.builder()
                            .text(aiResponse.toString())
                            .conversationId(request.getConversationId())
                            .role(MessageType.ASSISTANT.name())
                            .build();

                    messageRepository.saveAll(List.of(userMessage, aiMessage));

                    AssistantMessage aiMessage2 = new AssistantMessage(aiResponse.toString());
                    hybridChatMemoryRepository.saveAll(request.getConversationId(), List.of(userQuery, aiMessage2));

                    System.out.println("Saved AI message: " + aiMessage.getId());
                })
                .doOnError(err -> {
                    System.err.println("Stream error: " + err.getMessage());
                });
    }






    public MessageResponse create (MessageRequest request) throws AppException {
        Conversation conversation = conversationRepository.findById(request.getConversationId()).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if(!conversation.getUserId().equals(user.getId())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

//        Message message = messageMapper.toMessage(request);
//        message.setUserId(user.getId());

        UserMessage userQuery = new UserMessage(request.getText());
        org.springframework.ai.chat.messages.Message aiResponse = chatService.oneTimeResponse(chatService.generatePrompt(userQuery, request.getConversationId()));

        hybridChatMemoryRepository.saveAll(request.getConversationId(), List.of(userQuery, aiResponse));

        Message userMessage = Message.builder()
                .text(request.getText())
                .conversationId(request.getConversationId())
                .role(MessageType.USER.name())
                .build();
        Message aiMessage = Message.builder()
                .text(aiResponse.getText())
                .conversationId(request.getConversationId())
                .role(MessageType.ASSISTANT.name())
                .build();
        messageRepository.saveAll(List.of(userMessage, aiMessage));

        return MessageResponse.builder()
                .id(aiMessage.getId())
                .role(MessageType.ASSISTANT.name())
                .text(aiMessage.getText())
                .conversationId(aiMessage.getConversationId())
                .createdAt(aiMessage.getCreatedAt())
                .build();
    }



    public List<MessageResponse> findAll (String conversationId){
        return messageRepository.findAllByConversationId(conversationId)
                .stream().map(message -> MessageResponse.builder()
                                .id(message.getId())
                                .text(message.getText())
                                .role(message.getRole())
                                .createdAt(message.getCreatedAt())
                        .conversationId(message.getConversationId())
                        .build())
                .toList();
    }

    private MessageResponse toMessageResponse (Message message) throws AppException {
        MessageResponse messageResponse = messageMapper.toMessageResponse(message);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
//        if (messageResponse.getUserId().equals(user.getId())){
//            messageResponse.setMe(true);
//        } else {
//            messageResponse.setMe(false);
//        }
        return messageResponse;
    }
}
