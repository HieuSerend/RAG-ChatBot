package com.team14.chatbot.service.RagServiceImpl.calculator;

import com.team14.chatbot.service.RagServiceImpl.CalculatorService;
import com.team14.chatbot.service.RagServiceImpl.calculator.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculationServiceImpl implements CalculatorService {

    private final CalcEngine calcEngine;


    @Override
    public CalculationResponse calculate(CalculationRequest request) {
//        ReasoningResult rr = reasoningExtractor.extract(request.getUserQuery());

        CalculationResponse cr = calcEngine.calculate(request.getExpression());
//        ExplanationResult er = explainComposer.compose(rr, cr);

        return CalculationResponse.builder()
                .isSuccess(true)
                .value(cr.getValue())
                .expression(request.getExpression())
                .build();
    }
}
