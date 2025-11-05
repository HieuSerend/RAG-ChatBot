package com.team14.chatbot.controller;


import com.team14.chatbot.dto.request.ChatRequest;
import com.team14.chatbot.dto.request.UserCreationRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.ChatResponse;
import com.team14.chatbot.dto.response.UserResponse;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.service.RagService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RagService ragService;

    @PostMapping("")
    public ApiResponse<ChatResponse> chat (@RequestBody ChatRequest request) throws AppException {
        return ApiResponse.<ChatResponse>builder()
                .data(ragService.generateResponse(request.getQuestion())).build();
    }

}
