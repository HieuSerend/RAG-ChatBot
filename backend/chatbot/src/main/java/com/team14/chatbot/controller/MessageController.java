package com.team14.chatbot.controller;

import com.team14.chatbot.dto.request.MessageRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.MessageResponse;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.service.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MessageController {
    MessageService messageService;
    ChatClient chatClient;

    @PostMapping(value = "/stream-create", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCreate (@RequestBody MessageRequest request) throws AppException  {
        return messageService.streamingCreate(request);
    }

    @PostMapping("/create")
    public ApiResponse<MessageResponse> create (@RequestBody MessageRequest request) throws AppException {
        return ApiResponse.<MessageResponse>builder().data(messageService.create(request)).build();
    }

    @GetMapping("/list/{conversationId}")
    public ApiResponse<List<MessageResponse>> findAll (@PathVariable("conversationId") String conversationId){
        return ApiResponse.<List<MessageResponse>>builder().data(messageService.findAll(conversationId)).build();
    }
}
