package com.team14.chatbot.service.RagServiceImpl.orchestrator.executor;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.util.Map;

@Builder
@Getter
@ToString
public class PipelinePlan {

    // 1. Cấu hình Intent (để log hoặc direct response)
    private String intent;
    private String directResponse; // Nếu có cái này thì Executor trả về luôn

    // Query gốc của người dùng
    private String query;

    private boolean enableStepBack;
    private boolean enableHyde;

    // 2. Cấu hình các Module (Nullable - Null nghĩa là không chạy)

    // Cấu hình tìm kiếm
    private RetrievalConfig retrievalConfig;

    // Cấu hình tính toán
    private CalculationConfig calculationConfig;

    // Cấu hình sinh câu trả lời (Hầu như lúc nào cũng có)
    private GenerationConfig generationConfig;

    // --- Inner Config Classes ---

    @Builder @Getter
    public static class RetrievalConfig {
        private String query;
        private int topK;
        private boolean enableMultiQuery;
        private int multiQueryCount;
    }

    @Builder @Getter
    public static class CalculationConfig {
        private String expression;
        private Map<String, Object> variables;
    }

    @Builder @Getter
    public static class GenerationConfig {
        private String model;   // "GEMINI2.5", "QWEN2.53B"
        private String intent;
    }
}