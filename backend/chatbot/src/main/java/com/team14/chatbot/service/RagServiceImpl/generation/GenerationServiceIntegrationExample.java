package com.team14.chatbot.service.RagServiceImpl.generation;

import com.team14.chatbot.dto.request.GenerationRequest;
import com.team14.chatbot.enums.TaskType;
import com.team14.chatbot.service.RagServiceImpl.GenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Example service demonstrating how to integrate GenerationService
 * into existing RAG pipeline and other workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationServiceIntegrationExample {
    
    private final GenerationService generationService;
    
    /**
     * Example 1: Analyze user query intent before processing
     */
    public Map<String, Object> analyzeQueryIntent(String userQuery) {
        log.info("Analyzing intent for query: {}", userQuery);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.ANALYZE_INTENT)
                .userInput(userQuery)
                .build();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = generationService.generate(request, Map.class);
        
        log.info("Intent analysis result: {}", result);
        return result;
    }
    
    /**
     * Example 2: Check content safety before processing
     */
    public boolean isContentSafe(String content) {
        log.info("Checking content safety");
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.SAFETY_CHECK)
                .userInput(content)
                .build();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = generationService.generate(request, Map.class);
            
            Boolean isSafe = (Boolean) result.get("isSafe");
            String riskLevel = (String) result.get("riskLevel");
            
            log.info("Safety check result - Safe: {}, Risk: {}", isSafe, riskLevel);
            return isSafe != null && isSafe;
            
        } catch (Exception e) {
            log.error("Error in safety check, defaulting to safe", e);
            return true; // Fail-safe: allow by default if check fails
        }
    }
    
    /**
     * Example 3: Generate execution plan for complex query
     */
    public String generateExecutionPlan(String complexQuery) {
        log.info("Generating execution plan for: {}", complexQuery);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.GENERATE_PLAN)
                .userInput(complexQuery)
                .build();
        
        String plan = generationService.generate(request);
        
        log.info("Generated execution plan: {}", plan);
        return plan;
    }
    
    /**
     * Example 4: Explain a financial term
     */
    public String explainFinancialTerm(String term, String additionalContext) {
        log.info("Explaining term: {}", term);
        
        Map<String, Object> context = new HashMap<>();
        if (additionalContext != null && !additionalContext.isEmpty()) {
            context.put("additionalContext", additionalContext);
        }
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.EXPLAIN_TERM)
                .userInput(term)
                .context(context)
                .build();
        
        String explanation = generationService.generate(request);
        
        log.info("Generated explanation for: {}", term);
        return explanation;
    }
    
    /**
     * Example 5: Summarize retrieved documents
     */
    public String summarizeDocuments(String query, List<Document> documents) {
        log.info("Summarizing {} documents for query: {}", documents.size(), query);
        
        // Combine document texts
        String documentsText = documents.stream()
                .map(doc -> {
                    String text = doc.getText();
                    Map<String, Object> metadata = doc.getMetadata();
                    return String.format("--- Document ---\nContent: %s\nMetadata: %s\n", 
                            text, metadata);
                })
                .collect(Collectors.joining("\n"));
        
        Map<String, Object> context = new HashMap<>();
        context.put("documents", documentsText);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.SUMMARIZE_DOCS)
                .userInput(query)
                .context(context)
                .build();
        
        String summary = generationService.generate(request);
        
        log.info("Generated summary for {} documents", documents.size());
        return summary;
    }
    
    /**
     * Example 6: Interpret calculation result
     */
    public String interpretCalculation(String question, String calculationResult) {
        log.info("Interpreting calculation for question: {}", question);
        
        Map<String, Object> context = new HashMap<>();
        context.put("calculationResult", calculationResult);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.INTERPRET_CALCULATION)
                .userInput(question)
                .context(context)
                .build();
        
        String interpretation = generationService.generate(request);
        
        log.info("Generated interpretation");
        return interpretation;
    }
    
    /**
     * Example 7: Validate generated answer (LLM-as-a-Judge)
     */
    public Map<String, Object> validateAnswer(
            String question, 
            String generatedAnswer, 
            List<Document> sourceDocuments) {
        
        log.info("Validating answer for question: {}", question);
        
        String documentsText = sourceDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
        
        Map<String, Object> context = new HashMap<>();
        context.put("generatedAnswer", generatedAnswer);
        context.put("sourceDocuments", documentsText);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.JUDGE_CROSS_CHECK)
                .userInput(question)
                .context(context)
                .build();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> validation = generationService.generate(request, Map.class);
        
        log.info("Validation result: {}", validation);
        return validation;
    }
    
    /**
     * Example 8: Complete RAG workflow with validation
     */
    public String enhancedRagWorkflow(
            String userQuery, 
            List<Document> retrievedDocuments) {
        
        log.info("Starting enhanced RAG workflow for query: {}", userQuery);
        
        // Step 1: Check safety
        if (!isContentSafe(userQuery)) {
            log.warn("Query failed safety check");
            return "Xin lỗi, nội dung của bạn không phù hợp với hệ thống.";
        }
        
        // Step 2: Analyze intent
        Map<String, Object> intentResult = analyzeQueryIntent(userQuery);
        String intent = (String) intentResult.get("intent");
        
        if ("CHITCHAT".equals(intent) || "GREETING".equals(intent)) {
            log.info("Query is chitchat/greeting, skipping RAG");
            return "Xin chào! Tôi là trợ lý tài chính AI. Bạn có câu hỏi gì về tài chính không?";
        }
        
        // Step 3: Summarize documents
        String summary = summarizeDocuments(userQuery, retrievedDocuments);
        
        // Step 4: Generate answer (using existing ChatService or similar)
        // For this example, we'll use the summary as the answer
        String generatedAnswer = summary;
        
        // Step 5: Validate answer
        Map<String, Object> validation = validateAnswer(userQuery, generatedAnswer, retrievedDocuments);
        String recommendation = (String) validation.get("recommendation");
        
        if ("ACCEPT".equals(recommendation)) {
            log.info("Answer validated successfully");
            return generatedAnswer;
        } else if ("REVISE".equals(recommendation)) {
            log.warn("Answer needs revision");
            // In a real implementation, you would revise the answer
            return generatedAnswer + "\n\n(Lưu ý: Câu trả lời này có thể cần được xem xét thêm)";
        } else {
            log.error("Answer validation failed");
            return "Xin lỗi, tôi không thể tạo câu trả lời chính xác cho câu hỏi này.";
        }
    }
    
    /**
     * Example 9: Using with custom temperature
     */
    public String explainWithHighCreativity(String term) {
        log.info("Explaining term with high creativity: {}", term);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.EXPLAIN_TERM)
                .userInput(term)
                .temperature(0.9) // Override default temperature
                .build();
        
        return generationService.generate(request);
    }
    
    /**
     * Example 10: Using with specific model (for future multi-model support)
     */
    public String generateWithSpecificModel(String query, String modelName) {
        log.info("Generating with specific model: {}", modelName);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.EXPLAIN_TERM)
                .userInput(query)
                .specificModel(modelName) // Override model (currently ignored but ready for future)
                .build();
        
        return generationService.generate(request);
    }
}
