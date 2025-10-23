package com.team14.chatbot.controller;

import com.nimbusds.jose.JOSEException;
import com.team14.chatbot.dto.request.AuthenticateRequest;
import com.team14.chatbot.dto.request.LogoutRequest;
import com.team14.chatbot.dto.request.RefreshTokenRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.AuthenticateResponse;
import com.team14.chatbot.exception.AppException;
import com.team14.chatbot.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticateController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticateResponse> authenticate (@RequestBody AuthenticateRequest request) throws AppException {
        return ApiResponse.<AuthenticateResponse>builder()
                .data(authenticationService.authenticate(request)).build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout (@RequestBody LogoutRequest request) throws AppException, ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticateResponse> refreshToken (@RequestBody RefreshTokenRequest request) throws AppException, ParseException, JOSEException {
        return ApiResponse.<AuthenticateResponse>builder().data(authenticationService.refreshToken(request)).build();
    }
}
