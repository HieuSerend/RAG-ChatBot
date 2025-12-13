package com.team14.chatbot.enums;

/**
 * Enum representing the different intents that can be classified from user
 * queries.
 * Used by the Query Router to determine the appropriate processing pipeline.
 */
public enum QueryIntent {

    GREETING("Chào hỏi / giao tiếp xã giao (VD: \"Chào bạn\")"),

    KNOWLEDGE_QUERY("Tìm kiếm kiến thức (VD: \"Lãi suất là gì?\")"),
    ADVISORY("Xin lời khuyên (VD: \"Nên mua vàng hay Đô?\")"),
    CALCULATION("Tính toán (VD: \"Tôi muốn tính toán lãi suất ngân hàng\")"),
    BEHAVIORAL("Phân tích hành vi (VD: \"Tôi đang cảm thấy buồn bã khi nhìn vào cổ phiếu Vingroup\")"),

    UNCLEAR("Câu hỏi không rõ ràng (VD: \"Tôi không hiểu câu hỏi này\")"),
    MALICIOUS_CONTENT("Nội dung độc hại / không phù hợp");

    private final String description;

    QueryIntent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
