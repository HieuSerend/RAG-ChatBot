package com.team14.chatbot.service.RagModules.query_processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team14.chatbot.enums.QueryIntent;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    ObjectMapper objectMapper;

    public QueryProcessingService(@Qualifier("llamaCollabClient") ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== PROMPTS ====================

    private static final String QUERY_ROUTING_PROMPT = """
        Classify user query intent in the financial domain. A query may have multiple intents.
        
        Query: "%s"
        
        Valid intents:
        Financial:
        - KNOWLEDGE_QUERY: Ask for definitions or factual financial information.
        - ADVISORY: Ask for advice, recommendations, or opinions on financial decisions.
        - CALCULATION: Ask to calculate or compute financial values.
        - UNSUPPORTED: Financial-related but unclear, incomplete, or out of scope.
        
        Non-financial:
        - MALICIOUS_CONTENT: Illegal, fraudulent, or harmful financial requests.
        - NON_FINANCIAL: Not related to finance.
        
        Return JSON array only (no markdown):
        [{"intent":"INTENT","query":"QUERY","explanation":"SHORT_REASON"}]
        """;

    private static final String STEP_BACK_PROMPT = """
            Apply step-back reasoning: analyze the underlying concept and rewrite as a clearer, more specific question.

            Original: "%s"

            Return only the rewritten question in Vietnamese, no explanation.
            """;

    private static final String HYDE_PROMPT = """
            Generate a 3-5 sentence hypothetical document excerpt that directly answers the query. Use financial terminology and reference-style writing.

            Query: "%s"

            Return only the document excerpt in Vietnamese, no title or explanation.
            """;

    private static final String MULTI_QUERY_PROMPT = """
            Generate %d query variations with different wording/keywords but same meaning for search optimization.

            Original: "%s"

            Return numbered list (1., 2., 3...) in Vietnamese, queries only, no explanation.
            """;

    /**
     * Routes the query by classifying user intent.
     * Determines whether RAG pipeline is needed or direct LLM response is
     * sufficient.
     */
    public List<IntentTask> analyzeIntent(String query, String conversationHistory) {
        log.debug("Routing query: {}", query);

        String prompt = String.format(QUERY_ROUTING_PROMPT, query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseRoutingResponse(response, query);
        } catch (Exception e) {
            log.error("Error in query routing, defaulting to NON_FINANCIAL", e);
            return List.of(new IntentTask(QueryIntent.NON_FINANCIAL, query,
                    "Không thể phân tích câu hỏi, mặc định NON_FINANCIAL"));
        }
    }

    /**
     * Parses the JSON response from the routing LLM call.
     */
    private List<IntentTask> parseRoutingResponse(String response, String query) {
        try {
            // 1. Clean markdown
            String cleanResponse = response.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            if (cleanResponse.isEmpty() || cleanResponse.equals("[]"))
                return List.of();

            return objectMapper.readValue(cleanResponse, new TypeReference<List<IntentTask>>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse routing response, defaulting to NON_FINANCIAL", e);
            return List.of(new IntentTask(QueryIntent.NON_FINANCIAL, query,
                    "Không thể phân tích câu hỏi, mặc định NON_FINANCIAL"));
        }
    }

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

        String prompt = String.format(MULTI_QUERY_PROMPT, count, query);

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

}
