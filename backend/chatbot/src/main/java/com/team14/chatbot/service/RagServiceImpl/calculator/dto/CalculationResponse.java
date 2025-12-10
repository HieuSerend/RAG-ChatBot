package com.team14.chatbot.service.RagServiceImpl.calculator.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CalculationResponse {
    private boolean isSuccess;
    private String expression;
    private BigDecimal value;
}
