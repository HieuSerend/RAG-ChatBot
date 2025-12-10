package com.team14.chatbot.service.RagServiceImpl.calculator.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CalculationConfig {

    private String expression;

}
