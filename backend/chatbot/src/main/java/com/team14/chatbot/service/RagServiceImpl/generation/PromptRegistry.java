package com.team14.chatbot.service.RagServiceImpl.generation;

import com.team14.chatbot.enums.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for managing prompt templates for different task types.
 * Stores system prompts and builds user prompts with context injection.
 */
@Component
@Slf4j
public class PromptRegistry {

    private final Map<TaskType, String> systemPrompts = new HashMap<>();
    private final Map<TaskType, String> userPromptTemplates = new HashMap<>();

    public PromptRegistry() {
        initializePrompts();
    }

    /**
     * Initialize all prompt templates
     */
    private void initializePrompts() {
        // ANALYZE_INTENT prompts
        systemPrompts.put(TaskType.ANALYZE_INTENT, """
                Bạn là một hệ thống phân tích ý định người dùng chuyên nghiệp.
                Nhiệm vụ của bạn là phân tích câu hỏi và trích xuất:
                1. Ý định chính (intent): KNOWLEDGE_QUERY, GREETING, CHITCHAT, FOLLOW_UP, COMPLEX_QUERY, UNCLEAR
                2. Các thực thể quan trọng (entities): tên người, tổ chức, số liệu, ngày tháng, v.v.
                3. Mức độ tự tin (confidence): 0.0 - 1.0

                Trả về kết quả dưới dạng JSON với định dạng:
                {
                    "intent": "INTENT_TYPE",
                    "entities": [{"type": "ENTITY_TYPE", "value": "entity_value"}],
                    "confidence": 0.95,
                    "requiresRag": true/false,
                    "explanation": "Lý do phân tích"
                }
                """);

        userPromptTemplates.put(TaskType.ANALYZE_INTENT, """
                Phân tích câu hỏi sau:

                Câu hỏi: {userInput}

                {contextInfo}
                """);

        // GENERATE_PLAN prompts
        systemPrompts.put(TaskType.GENERATE_PLAN, """
                Bạn là một hệ thống lập kế hoạch thực thi (Planner) thông minh.
                Nhiệm vụ của bạn là phân tích yêu cầu phức tạp và tạo ra một kế hoạch thực thi từng bước.

                Mỗi bước trong kế hoạch phải có:
                - stepId: ID của bước (số nguyên)
                - action: Hành động cần thực hiện (RETRIEVE, CALCULATE, EXPLAIN, SUMMARIZE, etc.)
                - description: Mô tả chi tiết bước này làm gì
                - dependencies: Danh sách ID các bước phụ thuộc (nếu có)
                - parameters: Các tham số cần thiết

                Trả về JSON array theo định dạng:
                [
                    {
                        "stepId": 1,
                        "action": "RETRIEVE",
                        "description": "Tìm kiếm tài liệu về chủ đề X",
                        "dependencies": [],
                        "parameters": {"query": "..."}
                    }
                ]
                """);

        userPromptTemplates.put(TaskType.GENERATE_PLAN, """
                Tạo kế hoạch thực thi cho yêu cầu sau:

                Yêu cầu: {userInput}

                {contextInfo}
                """);

        // EXPLAIN_TERM prompts
        systemPrompts.put(TaskType.EXPLAIN_TERM, """
                Bạn là một chuyên gia giải thích khái niệm tài chính dễ hiểu.
                Nhiệm vụ của bạn là:
                1. Định nghĩa khái niệm một cách rõ ràng, chính xác
                2. Đưa ra ví dụ thực tế dễ hiểu
                3. Giải thích các thuật ngữ liên quan
                4. Sử dụng ngôn ngữ đơn giản, tránh quá chuyên môn

                Định dạng trả lời:
                - Sử dụng Markdown cho cấu trúc
                - In đậm các thuật ngữ quan trọng
                - Sử dụng bullet points cho các điểm chính
                - Sử dụng LaTeX ($$ $$) cho công thức toán học nếu cần

                Luôn trả lời bằng tiếng Việt.
                """);

        userPromptTemplates.put(TaskType.EXPLAIN_TERM, """
                Hãy giải thích khái niệm sau một cách dễ hiểu:

                Khái niệm: {userInput}

                {contextInfo}
                """);

        // SUMMARIZE_DOCS prompts
        systemPrompts.put(TaskType.SUMMARIZE_DOCS, """
                Bạn là một chuyên gia tóm tắt tài liệu chuyên nghiệp.
                Nhiệm vụ của bạn là:
                1. Đọc và hiểu các tài liệu được cung cấp
                2. Trích xuất các ý chính, thông tin quan trọng
                3. Tóm tắt ngắn gọn, rõ ràng, mạch lạc
                4. Giữ nguyên các số liệu, con số quan trọng
                5. Tổ chức thông tin theo cấu trúc logic

                Định dạng tóm tắt:
                - Bắt đầu với ý chính (1-2 câu)
                - Liệt kê các điểm quan trọng theo bullet points
                - Kết luận (nếu cần)

                Luôn trả lời bằng tiếng Việt.
                """);

        userPromptTemplates.put(TaskType.SUMMARIZE_DOCS, """
                Tóm tắt các tài liệu sau:

                Câu hỏi liên quan: {userInput}

                Tài liệu:
                {documents}

                {contextInfo}
                """);

        // CALCULATION_PLANNING prompts
        systemPrompts.put(TaskType.CALCULATION_PLANNING, """
                Bạn là trợ lý phân tích và lập kế hoạch tính toán.
                Nhiệm vụ:
                1) Phân tích câu hỏi người dùng.
                2) Xác định các đại lượng, biến, hằng số liên quan.
                3) Chuyển câu hỏi thành biểu thức toán học có thể tính bằng EvalEx (hỗ trợ +, -, *, /, ^, ngoặc).
                4) Liệt kê các bước reasoning ngắn gọn.

                Yêu cầu định dạng JSON:
                {
                  "expression": "<biểu thức toán học đơn giản>",
                  "reasoningSteps": ["bước 1", "bước 2", "..."]
                }

                Chỉ trả về JSON hợp lệ, không kèm giải thích khác.
                """);

        userPromptTemplates.put(TaskType.CALCULATION_PLANNING, """
                Phân tích và lập kế hoạch tính toán cho câu hỏi:
                {userInput}

                Trả về JSON với expression và reasoningSteps.
                """);

        // INTERPRET_CALCULATION prompts
        systemPrompts.put(TaskType.INTERPRET_CALCULATION, """
                Bạn là một chuyên gia diễn giải kết quả tính toán tài chính.
                Nhiệm vụ của bạn là:
                1. Nhận kết quả tính toán số học/tài chính
                2. Diễn giải ý nghĩa của kết quả
                3. Đưa ra nhận xét, phân tích nếu cần
                4. Giải thích các công thức đã sử dụng

                Định dạng:
                - Trình bày kết quả rõ ràng
                - Giải thích ý nghĩa bằng ngôn ngữ dễ hiểu
                - Đưa ra ngữ cảnh thực tế nếu phù hợp

                Luôn trả lời bằng tiếng Việt.
                """);

        userPromptTemplates.put(TaskType.INTERPRET_CALCULATION, """
                Diễn giải kết quả tính toán sau:

                Câu hỏi ban đầu: {userInput}

                Kết quả tính toán:
                {calculationResult}

                {contextInfo}
                """);

        // SAFETY_CHECK prompts
        systemPrompts.put(TaskType.SAFETY_CHECK, """
                Bạn là một hệ thống kiểm tra an toàn nội dung (Content Moderator).
                Nhiệm vụ của bạn là phát hiện:
                1. Nội dung độc hại (toxic, hate speech)
                2. Spam, quảng cáo rác
                3. Thông tin sai lệch nguy hiểm
                4. Yêu cầu bất hợp pháp
                5. Nội dung không phù hợp với hệ thống tài chính

                Trả về JSON:
                {
                    "isSafe": true/false,
                    "riskLevel": "LOW|MEDIUM|HIGH",
                    "violations": ["type1", "type2"],
                    "reason": "Lý do cụ thể"
                }
                """);

        userPromptTemplates.put(TaskType.SAFETY_CHECK, """
                Kiểm tra an toàn cho nội dung sau:

                Nội dung: {userInput}

                {contextInfo}
                """);

        // JUDGE_CROSS_CHECK prompts
        systemPrompts.put(TaskType.JUDGE_CROSS_CHECK, """
                Bạn là một hệ thống đánh giá chất lượng câu trả lời (LLM-as-a-Judge).
                Nhiệm vụ của bạn là kiểm tra tính đúng đắn của câu trả lời dựa trên:
                1. Tính chính xác: Thông tin có đúng với tài liệu nguồn?
                2. Tính đầy đủ: Câu trả lời có đủ thông tin?
                3. Tính nhất quán: Có mâu thuẫn nội bộ không?
                4. Tính liên quan: Có trả lời đúng câu hỏi không?

                Trả về JSON:
                {
                    "isCorrect": true/false,
                    "accuracy": 0.95,
                    "completeness": 0.90,
                    "relevance": 1.0,
                    "issues": ["issue1", "issue2"],
                    "recommendation": "ACCEPT|REJECT|REVISE",
                    "reason": "..."
                }
                """);

        userPromptTemplates.put(TaskType.JUDGE_CROSS_CHECK, """
                Đánh giá câu trả lời sau:

                Câu hỏi: {userInput}

                Câu trả lời cần đánh giá:
                {generatedAnswer}

                Tài liệu nguồn:
                {sourceDocuments}

                {contextInfo}
                """);

        // FUSION prompts
        systemPrompts.put(TaskType.FUSION, """
                Bạn là hệ thống tổng hợp câu trả lời.
                Nhiệm vụ:
                - Hợp nhất thông tin từ nhiều câu trả lời thành phần
                - Loại bỏ trùng lặp, giải quyết mâu thuẫn
                - Trả lời ngắn gọn, chính xác, mạch lạc
                Luôn trả lời bằng tiếng Việt.
                """);

        userPromptTemplates.put(TaskType.FUSION, """
                Câu hỏi gốc: {userInput}

                Các câu trả lời thành phần:
                {answers}

                {contextInfo}
                """);

        // FUSION_SELF_CORRECT prompts
        systemPrompts.put(TaskType.FUSION_SELF_CORRECT, """
                Bạn là hệ thống tự hiệu chỉnh câu trả lời sau khi bị đánh giá chưa đạt.
                Nhiệm vụ:
                - Đọc câu hỏi gốc và các câu trả lời thành phần
                - Hiểu lý do validator đánh giá chưa đạt
                - Sửa lỗi, bổ sung thông tin thiếu, làm rõ điểm mâu thuẫn
                - Trả lời ngắn gọn, chính xác, mạch lạc, tiếng Việt
                """);

        userPromptTemplates.put(TaskType.FUSION_SELF_CORRECT, """
                Câu hỏi gốc: {userInput}

                Các câu trả lời thành phần:
                {answers}

                Lý do validator chê:
                {validatorReason}

                Chế độ: {mode}

                {contextInfo}
                """);
    }

    /**
     * Get system prompt for a task type
     */
    public String getSystemPrompt(TaskType taskType) {
        String prompt = systemPrompts.get(taskType);
        if (prompt == null) {
            log.warn("No system prompt found for task type: {}, using default", taskType);
            return "Bạn là một trợ lý AI chuyên nghiệp. Hãy thực hiện nhiệm vụ được yêu cầu.";
        }
        return prompt;
    }

    /**
     * Build user prompt by rendering template with context
     */
    public String buildUserPrompt(TaskType taskType, String userInput, Map<String, Object> context) {
        String template = userPromptTemplates.get(taskType);
        if (template == null) {
            log.warn("No user prompt template found for task type: {}, using input directly", taskType);
            return userInput;
        }

        // Simple template rendering
        String rendered = template.replace("{userInput}", userInput != null ? userInput : "");

        // Inject context variables
        if (context != null && !context.isEmpty()) {
            StringBuilder contextInfo = new StringBuilder();

            // Handle specific context keys
            if (context.containsKey("documents")) {
                contextInfo.append("Tài liệu tham khảo:\n").append(context.get("documents")).append("\n\n");
            }

            if (context.containsKey("conversationHistory")) {
                contextInfo.append("Lịch sử hội thoại:\n").append(context.get("conversationHistory")).append("\n\n");
            }

            if (context.containsKey("calculationResult")) {
                contextInfo.append("Kết quả:\n").append(context.get("calculationResult")).append("\n\n");
            }

            if (context.containsKey("generatedAnswer")) {
                contextInfo.append("Câu trả lời:\n").append(context.get("generatedAnswer")).append("\n\n");
            }

            if (context.containsKey("sourceDocuments")) {
                contextInfo.append("Nguồn:\n").append(context.get("sourceDocuments")).append("\n\n");
            }

            // Add any remaining context as additional info
            context.forEach((key, value) -> {
                if (!key.equals("documents") && !key.equals("conversationHistory") &&
                        !key.equals("calculationResult") && !key.equals("generatedAnswer") &&
                        !key.equals("sourceDocuments")) {
                    contextInfo.append(key).append(": ").append(value).append("\n");
                }
            });

            rendered = rendered.replace("{contextInfo}", contextInfo.toString().trim());
        } else {
            rendered = rendered.replace("{contextInfo}", "");
        }

        // Replace any remaining template variables with empty string
        rendered = rendered.replaceAll("\\{[^}]+\\}", "");

        return rendered;
    }
}
