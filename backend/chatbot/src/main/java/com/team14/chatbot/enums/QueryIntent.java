package com.team14.chatbot.enums;

/**
 * Enum representing the different intents that can be classified from user
 * queries.
 * Used by the Query Router to determine the appropriate processing pipeline.
 */
public enum QueryIntent {

    // --- NHÓM RAG (Cần tìm kiếm & Tổng hợp) ---
    FACTUAL_LOOKUP("Hỏi sự kiện, định nghĩa (VD: \"Lãi suất là gì?\")"),
    COMPARISON("So sánh (VD: \"Vingroup khác gì Viettel?\")"),
    ADVISORY("Xin lời khuyên (VD: \"Nên mua vàng hay Đô?\")"),
    SUMMARIZATION("Tóm tắt (VD: \"Tóm tắt văn bản này\")"),

    // --- NHÓM CÔNG CỤ (Cần Calculator/Code) ---
    CALCULATION("Tính toán (VD: \"100tr lãi 5% trong 10 năm\")"),
    DATA_ANALYSIS("Phân tích số liệu (VD: \"Vẽ biểu đồ doanh thu\")"),

    // --- NHÓM XÃ GIAO / HỆ THỐNG ---
    CHIT_CHAT("Chào hỏi / giao tiếp xã giao"),
    SYSTEM_COMMAND("Lệnh hệ thống (Xóa history, Đổi ngôn ngữ)"),

    // --- NHÓM NGOẠI LỆ ---
    AMBIGUOUS("Mơ hồ - cần làm rõ"),
    TOXIC_CONTENT("Nội dung độc hại / không phù hợp"),
    KNOWLEDGE_QUERY("");

    private final String description;

    QueryIntent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
