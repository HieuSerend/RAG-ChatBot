package com.team14.chatbot.service.RagServiceImpl.orchestrator;

import com.team14.chatbot.service.RagServiceImpl.query_processor.RoutingResult;

public interface QueryProcessingService {
    /**
     * Pipeline xử lý câu hỏi thô:
     * 1. Routing (Phân loại Intent).
     * 2. Transformation (Step-back / HyDE).
     * 3. Expansion (Multi-query).
     * * @param rawQuery Câu hỏi gốc từ người dùng.
     * @return Context chứa Intent và danh sách các câu query đã tối ưu cho Search.
     */
//    ProcessedQueryContext process(String rawQuery);

    RoutingResult analyzeIntent(String query, String conversationHistory);
}
