package com.team14.chatbot.service.RagServiceImpl.validator;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ValidationResponse {
    private boolean isValid;         // Pass hay Fail?

    private String reason;           // Lý do (từ LLM giải thích)

    public static ValidationResponse valid() {
        return new ValidationResponse(true, "OK");
    }

    public static ValidationResponse invalid(String reason) {
        return new ValidationResponse(false, reason);
    }
}
