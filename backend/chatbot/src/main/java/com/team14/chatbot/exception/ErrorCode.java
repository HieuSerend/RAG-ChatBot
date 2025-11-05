package com.team14.chatbot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNAUTHENTICATED(1001, "authenticated error", HttpStatus.UNAUTHORIZED),
    USER_EXISTED(1002, "user existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "user not existed", HttpStatus.NOT_FOUND),
    ROLE_NOT_EXISTED(1004, "role not existed", HttpStatus.NOT_FOUND),
    UNAUTHORIZED(1005, "You do not have permission", HttpStatus.FORBIDDEN),
    UNCATEGORIZED(1006, "uncategorized exception", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_EXISTED(1007, "conversation not existed", HttpStatus.NOT_FOUND)
    ;


    private long code;
    private String message;
    private HttpStatusCode httpStatusCode;

    ErrorCode(long code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
