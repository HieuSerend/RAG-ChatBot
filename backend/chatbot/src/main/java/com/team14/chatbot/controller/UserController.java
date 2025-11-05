package com.team14.chatbot.controller;


import com.team14.chatbot.dto.request.UserCreationRequest;
import com.team14.chatbot.dto.request.UserUpdationRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.UserResponse;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> createUser (@RequestBody UserCreationRequest request) throws AppException {
        return ApiResponse.<UserResponse>builder()
                .data(userService.createUser(request)).build();
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo () throws AppException {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getMyInfo()).build();
    }

    @PutMapping("/update-my-info")
    public ApiResponse<UserResponse> updateMyInfo (@RequestBody UserUpdationRequest request) throws AppException {
        return ApiResponse.<UserResponse>builder().data(userService.updateMyProfile(request)).build();
    }

}
