package com.team14.chatbot.controller;

import com.team14.chatbot.dto.request.ConversationRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.ConversationResponse;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.service.ConversationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ConversationController {
    ConversationService conversationService;

    @PostMapping("/create")
    public ApiResponse<ConversationResponse> create (@RequestBody ConversationRequest request) throws AppException {
        return ApiResponse.<ConversationResponse>builder().data(conversationService.create(request)).build();
    }

    @GetMapping("/list")
    public ApiResponse<List<ConversationResponse>> findAll (){
        return ApiResponse.<List<ConversationResponse>>builder().data(conversationService.findAll()).build();
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete (@PathVariable("id") String id){
        conversationService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
