package com.team14.chatbot.service.RagServiceImpl.query_processor;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class IntentAnalysisResult {
    private String originalPrompt;
    private List<IntentPlan> intents;
    // true nếu có thể chạy các intent song song bằng virtual threads
    private boolean intentsRunnableInParallel;
}

