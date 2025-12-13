package com.team14.chatbot.service.RagServiceImpl.query_processor;

import com.team14.chatbot.enums.QueryIntent;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Query Processing Service - Core component for intelligent query handling
 * 
 * This service implements three main stages:
 * 1. Query Routing: Classifies user intent and decides processing pipeline
 * 2. Query Transformation: Applies step-back prompting and HyDE
 * 3. Query Expansion: Generates multiple query variations for better retrieval
 */
@Service
// @RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QueryProcessingService {

    ChatClient chatClient;

    public QueryProcessingService(@Qualifier("llamaCollabClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // ==================== PROMPTS ====================

    private static final String QUERY_ROUTING_PROMPT = """
        Bạn là hệ thống phân loại ý định người dùng.
        
        Phân tích câu hỏi và (nếu có) lịch sử hội thoại, rồi trả về JSON.
        
        Câu hỏi: "%s"
        Lịch sử: %s
        
        Chọn MỘT trong các intent:
        - KNOWLEDGE_QUERY
        - GREETING
        - ADVISORY
        - CALCULATION
        - BEHAVIORAL
        - UNCLEAR
        - MALICIOUS_CONTENT
        
        Trả về đúng format (không markdown, không giải thích thêm):
        {"intent":"INTENT","explanation":"Mô tả ngắn gọn"}
        """;

    private static final String STEP_BACK_PROMPT = """
            Bạn là một chuyên gia về tư duy phân tích. Nhiệm vụ của bạn là "lùi lại một bước" (step-back)
            để hiểu bản chất sâu hơn của câu hỏi.

            Câu hỏi gốc: "%s"

            Hãy phân tích:
            1. Câu hỏi này thực sự đang hỏi về vấn đề gì ở mức cao hơn?
            2. Những khái niệm nền tảng nào cần hiểu để trả lời câu hỏi này?

            Sau đó, viết lại thành một câu hỏi RÕ RÀNG và CỤ THỂ hơn, tập trung vào bản chất của vấn đề.

            Chỉ trả về câu hỏi mới, không giải thích thêm.

            Câu hỏi mới:
            """;

    private static final String HYDE_PROMPT = """
            Bạn là một chuyên gia trong lĩnh vực tài chính và đầu tư.

            Câu hỏi: "%s"

            Hãy tưởng tượng và viết một đoạn văn ngắn (3-5 câu) như thể đây là một đoạn trích từ
            tài liệu chuyên môn trả lời trực tiếp cho câu hỏi trên.

            Đoạn văn nên:
            - Chứa các thuật ngữ chuyên môn liên quan
            - Đề cập đến các khái niệm chính
            - Có phong cách như một tài liệu tham khảo

            Chỉ viết đoạn văn, không có tiêu đề hay giải thích:
            """;

    private static final String MULTI_QUERY_PROMPT = """
            Bạn là một chuyên gia về tối ưu hóa truy vấn tìm kiếm.

            Câu hỏi gốc: "%s"

            Hãy tạo ra %d câu hỏi tương tự nhưng được diễn đạt khác nhau,
            mỗi câu nhấn mạnh một khía cạnh hoặc sử dụng từ khóa khác.

            Mục đích: Các câu hỏi này sẽ được dùng để tìm kiếm, nên cần đa dạng về từ khóa
            nhưng vẫn giữ nguyên ý nghĩa.

            Format: Mỗi câu hỏi trên một dòng, đánh số 1, 2, 3...
            Chỉ liệt kê các câu hỏi, không giải thích thêm.
            """;

    private static final String DIRECT_RESPONSE_PROMPT = """
            Bạn là một trợ lý AI thân thiện và chuyên nghiệp.

            Người dùng nói: "%s"

            Lịch sử hội thoại: %s

            Hãy trả lời một cách tự nhiên, thân thiện và phù hợp với ngữ cảnh.
            Nếu đây là lời chào, hãy chào lại và hỏi có thể giúp gì.
            Nếu đây là câu hỏi ngoài lề, hãy trả lời ngắn gọn rồi gợi ý quay lại chủ đề chính.
            """;

    // ==================== MAIN PROCESSING METHOD ====================

    /**
     * Main entry point for query processing.
     * Orchestrates routing, transformation, and expansion stages.
     */
    /*
     * public QueryProcessingResponse processQuery(QueryProcessingRequest request) {
     * long startTime = System.currentTimeMillis();
     * log.info("Starting query processing for: {}", request.getQuery());
     * 
     * // Stage 1: Query Routing
     * RoutingResult routingResult = routeQuery(request.getQuery(),
     * request.getConversationHistory());
     * 
     * QueryProcessingResponse.QueryProcessingResponseBuilder responseBuilder =
     * QueryProcessingResponse.builder()
     * .originalQuery(request.getQuery())
     * .intent(routingResult.intent())
     * .routingDecision(RoutingDecision.builder()
     * .requiresRag(routingResult.requiresRag())
     * .confidence(routingResult.confidence())
     * .targetPipeline(routingResult.requiresRag() ? "RAG" : "DIRECT_LLM")
     * .build())
     * .routingExplanation(routingResult.explanation());
     * 
     * // If no RAG needed, get direct response and return early
     * if (!routingResult.requiresRag()) {
     * String directResponse = generateDirectResponse(request.getQuery(),
     * request.getConversationHistory());
     * responseBuilder.directResponse(directResponse);
     * responseBuilder.processingTimeMs(System.currentTimeMillis() - startTime);
     * log.info("Query routed to direct LLM response (no RAG needed)");
     * return responseBuilder.build();
     * }
     * 
     * // Stage 2: Query Transformation
     * String stepBackQuery = null;
     * String hypotheticalDoc = null;
     * 
     * if (request.isEnableStepBack()) {
     * stepBackQuery = transformWithStepBack(request.getQuery());
     * responseBuilder.stepBackQuery(stepBackQuery);
     * log.info("Step-back query: {}", stepBackQuery);
     * }
     * 
     * if (request.isEnableHyde()) {
     * // Use step-back query if available, otherwise original query
     * String queryForHyde = stepBackQuery != null ? stepBackQuery :
     * request.getQuery();
     * hypotheticalDoc = generateHypotheticalDocument(queryForHyde);
     * responseBuilder.hypotheticalDocument(hypotheticalDoc);
     * log.info("Generated hypothetical document");
     * }
     * 
     * // Stage 3: Query Expansion
     * List<String> expandedQueries = new ArrayList<>();
     * if (request.isEnableMultiQuery()) {
     * String queryToExpand = stepBackQuery != null ? stepBackQuery :
     * request.getQuery();
     * expandedQueries = expandQuery(queryToExpand, request.getMultiQueryCount());
     * responseBuilder.expandedQueries(expandedQueries);
     * log.info("Generated {} expanded queries", expandedQueries.size());
     * }
     * 
     * // Determine the optimized query (best query for retrieval)
     * String optimizedQuery = determineOptimizedQuery(
     * request.getQuery(), stepBackQuery, hypotheticalDoc, expandedQueries);
     * responseBuilder.optimizedQuery(optimizedQuery);
     * 
     * responseBuilder.processingTimeMs(System.currentTimeMillis() - startTime);
     * 
     * return responseBuilder.build();
     * }
     * 
     */

    // ==================== STAGE 1: QUERY ROUTING ====================

    /**
     * Routes the query by classifying user intent.
     * Determines whether RAG pipeline is needed or direct LLM response is
     * sufficient.
     */
    public RoutingResult analyzeIntent(String query, String conversationHistory) {
        log.debug("Routing query: {}", query);

        String historyContext = conversationHistory != null ? conversationHistory : "Không có";
        String prompt = String.format(QUERY_ROUTING_PROMPT, query, historyContext);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseRoutingResponse(response);
        } catch (Exception e) {
            log.error("Error in query routing, defaulting to RAG pipeline", e);
            return new RoutingResult(QueryIntent.ADVISORY,
                    "Mặc định sử dụng RAG do lỗi phân loại");
        }
    }

    /**
     * Parses the JSON response from the routing LLM call.
     */
    private RoutingResult parseRoutingResponse(String response) {
        try {
            // Clean up response (remove markdown if present)
            String cleanResponse = response.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            // Simple JSON parsing (avoiding external dependencies)
            String intent = extractJsonValue(cleanResponse, "intent");
            String explanation = extractJsonValue(cleanResponse, "explanation");

            QueryIntent queryIntent = QueryIntent.valueOf(intent);

            return new RoutingResult(queryIntent, explanation);
        } catch (Exception e) {
            log.warn("Failed to parse routing response: {}, defaulting to UNCLEAR", response);
            return new RoutingResult(QueryIntent.UNCLEAR,
                    "Không thể phân tích response, mặc định UNCLEAR");
        }
    }

    /**
     * Simple JSON value extractor (to avoid adding Jackson dependencies for simple
     * parsing)
     */
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"\\}]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    // ==================== STAGE 2: QUERY TRANSFORMATION ====================

    /**
     * Applies Step-Back Prompting to transform the query.
     * Creates a more abstract, general version of the query that captures the
     * underlying concept.
     */
    public String transformWithStepBack(String query) {
        log.debug("Applying step-back prompting to: {}", query);

        String prompt = String.format(STEP_BACK_PROMPT, query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return response.trim();
        } catch (Exception e) {
            log.error("Error in step-back transformation", e);
            return query; // Return original query on error
        }
    }

    /**
     * Generates a Hypothetical Document using HyDE technique.
     * The generated document contains relevant keywords and concepts for better
     * semantic search.
     */
    public String generateHypotheticalDocument(String query) {
        log.debug("Generating hypothetical document for: {}", query);

        String prompt = String.format(HYDE_PROMPT, query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return response.trim();
        } catch (Exception e) {
            log.error("Error in HyDE generation", e);
            return null;
        }
    }

    // ==================== STAGE 3: QUERY EXPANSION ====================

    /**
     * Expands the query into multiple variations using Multi-Query technique.
     * Each variation emphasizes different aspects or uses alternative keywords.
     */
    public List<String> expandQueryForExecutor(String query, int count) {
        log.debug("Expanding query: {} into {} variations", query, count);

        String prompt = String.format(MULTI_QUERY_PROMPT, query, count);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseExpandedQueries(response);
        } catch (Exception e) {
            log.error("Error in query expansion", e);
            return List.of(query); // Return original query on error
        }
    }

    /**
     * Parses the numbered list of expanded queries from LLM response.
     */
    private List<String> parseExpandedQueries(String response) {
        List<String> queries = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            // Remove numbering (1., 2., 3., etc.) and clean up
            String cleaned = line.trim()
                    .replaceFirst("^\\d+\\.?\\s*", "")
                    .replaceFirst("^-\\s*", "")
                    .trim();

            if (!cleaned.isEmpty()) {
                queries.add(cleaned);
            }
        }

        return queries;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Determines the best optimized query for retrieval based on all
     * transformations.
     */
    private String determineOptimizedQuery(String original, String stepBack,
            String hypotheticalDoc, List<String> expanded) {
        // Priority: Step-back query > Original query
        // The hypothetical document and expanded queries are used alongside, not as
        // replacement
        if (stepBack != null && !stepBack.isEmpty() && !stepBack.equals(original)) {
            return stepBack;
        }
        return original;
    }

    /**
     * Generates a direct response for queries that don't need RAG.
     */
    private String generateDirectResponse(String query, String conversationHistory) {
        String historyContext = conversationHistory != null ? conversationHistory : "Không có";
        String prompt = String.format(DIRECT_RESPONSE_PROMPT, query, historyContext);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error generating direct response", e);
            return "Xin lỗi, tôi gặp sự cố khi xử lý yêu cầu của bạn. Vui lòng thử lại.";
        }
    }

    // ==================== INNER CLASSES ====================

    // ==================== INTENT ANALYZER (multi-intent + multi-query plan)
    // ====================

    /**
     * Phân tích prompt người dùng, tách thành nhiều intent (nếu có) và gợi ý chiến
     * lược multi-query,
     * đồng thời cung cấp metadata cho virtual threads.
     */
    public IntentAnalysisResult analyzeMultiIntent(String prompt) {
        List<String> rawIntents = splitIntoIntentCandidates(prompt);
        List<IntentPlan> plans = rawIntents.stream()
                .map(this::buildIntentPlan)
                .collect(Collectors.toList());

        boolean parallelIntents = plans.size() > 1;

        return IntentAnalysisResult.builder()
                .originalPrompt(prompt)
                .intents(plans)
                .intentsRunnableInParallel(parallelIntents)
                .build();
    }

    private List<String> splitIntoIntentCandidates(String prompt) {
        // Tách theo ?, ., ; và xuống dòng, lọc chuỗi rỗng
        String[] parts = prompt.split("[?。．。!;\\n]");
        List<String> intents = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                intents.add(trimmed);
            }
        }
        // Nếu không tách được, giữ nguyên prompt
        if (intents.isEmpty())
            intents.add(prompt.trim());
        return intents;
    }

    private IntentPlan buildIntentPlan(String intentText) {
        String taskType = classifyTaskType(intentText);

        // Heuristic multi-query: bật khi intent có nhiều vế hoặc từ khóa rộng
        boolean useMultiQuery = isBroadIntent(intentText);
        List<SubQueryPlan> subQueries = useMultiQuery ? buildSubQueries(intentText) : List.of();
        String fusionRule = useMultiQuery ? suggestFusionRule(taskType) : "none";

        return IntentPlan.builder()
                .intentDescription(intentText)
                .taskType(taskType)
                .requiredInputs("user_prompt, conversation_history, domain_constraints")
                .expectedOutputs("structured_answer or action plan tailored to taskType")
                .subQueries(subQueries)
                .fusionRule(fusionRule)
                .subQueriesRunnableInParallel(useMultiQuery)
                .build();
    }

    private String classifyTaskType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("tính") || lower.contains("tính toán") || lower.matches(".*\\d+.*%.*")) {
            return "calculation";
        }
        if (lower.contains("viết code") || lower.contains("code") || lower.contains("lập trình")) {
            return "coding";
        }
        if (lower.contains("phân tích") || lower.contains("so sánh")) {
            return "analysis";
        }
        if (lower.contains("tóm tắt") || lower.contains("tổng hợp") || lower.contains("summar")) {
            return "summarization";
        }
        if (lower.contains("viết lại") || lower.contains("rewrite") || lower.contains("paraphrase")) {
            return "rewrite";
        }
        if (lower.contains("gợi ý") || lower.contains("khuyên") || lower.contains("nên làm gì")) {
            return "advisory";
        }
        return "knowledge_query";
    }

    private boolean isBroadIntent(String text) {
        String lower = text.toLowerCase();
        int andCount = countOccurrences(lower, " và ") + countOccurrences(lower, " and ");
        boolean hasBroadKeywords = lower.contains("tổng quan") || lower.contains("so sánh")
                || lower.contains("ưu nhược") || lower.contains("phân tích");
        return andCount > 0 || hasBroadKeywords;
    }

    private int countOccurrences(String haystack, String needle) {
        int idx = 0;
        int count = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private List<SubQueryPlan> buildSubQueries(String intentText) {
        List<SubQueryPlan> subs = new ArrayList<>();
        String[] chunks = intentText.split("\\b(và|and)\\b");
        if (chunks.length > 1) {
            for (String c : chunks) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty()) {
                    subs.add(SubQueryPlan.builder()
                            .query(trimmed)
                            .purpose("focus_on_component")
                            .build());
                }
            }
        }
        if (subs.isEmpty()) {
            subs.add(SubQueryPlan.builder()
                    .query(intentText)
                    .purpose("core_query")
                    .build());
            subs.add(SubQueryPlan.builder()
                    .query("contextual background for: " + intentText)
                    .purpose("background")
                    .build());
        }
        return subs;
    }

    private String suggestFusionRule(String taskType) {
        return switch (taskType) {
            case "analysis" -> "intersection_then_ranking";
            case "summarization", "knowledge_query", "advisory" -> "union_then_ranking";
            case "calculation" -> "consistency_check";
            default -> "union";
        };
    }

}
