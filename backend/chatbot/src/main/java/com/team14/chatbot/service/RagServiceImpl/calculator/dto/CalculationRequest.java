package com.team14.chatbot.service.RagServiceImpl.calculator.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalculationRequest {
    String expression;// Làm tròn mấy số thập phân?
}