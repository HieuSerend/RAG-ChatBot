package com.team14.chatbot.service.RagServiceImpl.calculator;

import com.team14.chatbot.service.RagServiceImpl.calculator.dto.CalculationResponse;
import com.udojava.evalex.Expression;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalcEngine {

    public CalculationResponse calculate(String expr) {
        try {
            Expression expression = new Expression(expr);
            BigDecimal result = expression.eval();

            return CalculationResponse.builder()
                    .isSuccess(true)
                    .expression(expr)
                    .value(result)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating expression: " + expr, e);

        }

    }
}
