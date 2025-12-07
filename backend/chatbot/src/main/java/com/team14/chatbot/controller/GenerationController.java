package com.team14.chatbot.controller;

import com.team14.chatbot.dto.request.GenerationRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.GenerationResponse;
import com.team14.chatbot.enums.TaskType;
import com.team14.chatbot.service.generation.GenerationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for GenerationService.
 * Provides endpoints to test and use the unified generation service.
 */
@RestController
@RequestMapping("/api/generation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@CrossOrigin(origins = "*")
public class GenerationController {

    GenerationService generationService;

    /**
     * General generation endpoint - accepts any task type
     */
    @PostMapping("/generate")
    public ApiResponse<String> generate(@RequestBody GenerationRequest request) {
        log.info("Received generation request for task type: {}", request.getTaskType());
        
        String response = generationService.generate(request);
        
        return ApiResponse.<String>builder()
                .data(response)
                .message("Generation completed successfully")
                .build();
    }

    /**
     * Generate with metadata - returns detailed response information
     */
    @PostMapping("/generate-with-metadata")
    public ApiResponse<GenerationResponse<String>> generateWithMetadata(@RequestBody GenerationRequest request) {
        log.info("Received generation request with metadata for task type: {}", request.getTaskType());
        
        GenerationResponse<String> response = generationService.generateWithMetadata(request, String.class);
        
        return ApiResponse.<GenerationResponse<String>>builder()
                .data(response)
                .message("Generation completed successfully")
                .build();
    }

    /**
     * Analyze intent - specialized endpoint for intent analysis
     */
    @PostMapping("/analyze-intent")
    public ApiResponse<Map<String, Object>> analyzeIntent(@RequestParam String query) {
        log.info("Analyzing intent for query: {}", query);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.ANALYZE_INTENT)
                .userInput(query)
                .build();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = generationService.generate(request, Map.class);
        
        return ApiResponse.<Map<String, Object>>builder()
                .data(result)
                .message("Intent analysis completed")
                .build();
    }

    /**
     * Explain term - specialized endpoint for concept explanation
     */
    @PostMapping("/explain")
    public ApiResponse<String> explainTerm(
            @RequestParam String term,
            @RequestParam(required = false) String context) {
        log.info("Explaining term: {}", term);
        
        GenerationRequest.GenerationRequestBuilder requestBuilder = GenerationRequest.builder()
                .taskType(TaskType.EXPLAIN_TERM)
                .userInput(term);
        
        if (context != null && !context.isEmpty()) {
            Map<String, Object> contextMap = new HashMap<>();
            contextMap.put("additionalContext", context);
            requestBuilder.context(contextMap);
        }
        
        String response = generationService.generate(requestBuilder.build());
        
        return ApiResponse.<String>builder()
                .data(response)
                .message("Explanation generated successfully")
                .build();
    }

    /**
     * Summarize documents - specialized endpoint for document summarization
     */
    @PostMapping("/summarize")
    public ApiResponse<String> summarizeDocuments(
            @RequestParam String query,
            @RequestBody String documents) {
        log.info("Summarizing documents for query: {}", query);
        
        Map<String, Object> context = new HashMap<>();
        context.put("documents", documents);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.SUMMARIZE_DOCS)
                .userInput(query)
                .context(context)
                .build();
        
        String response = generationService.generate(request);
        
        return ApiResponse.<String>builder()
                .data(response)
                .message("Summarization completed successfully")
                .build();
    }

    /**
     * Safety check - specialized endpoint for content moderation
     */
    @PostMapping("/safety-check")
    public ApiResponse<Map<String, Object>> safetyCheck(@RequestParam String content) {
        log.info("Performing safety check on content");
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.SAFETY_CHECK)
                .userInput(content)
                .build();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = generationService.generate(request, Map.class);
        
        return ApiResponse.<Map<String, Object>>builder()
                .data(result)
                .message("Safety check completed")
                .build();
    }

    /**
     * Generate plan - specialized endpoint for execution planning
     */
    @PostMapping("/generate-plan")
    public ApiResponse<Object> generatePlan(
            @RequestParam String requirement,
            @RequestBody(required = false) Map<String, Object> context) {
        log.info("Generating execution plan for requirement: {}", requirement);
        
        GenerationRequest.GenerationRequestBuilder requestBuilder = GenerationRequest.builder()
                .taskType(TaskType.GENERATE_PLAN)
                .userInput(requirement);
        
        if (context != null) {
            requestBuilder.context(context);
        }
        
        // Try to parse as array or object
        String response = generationService.generate(requestBuilder.build());
        
        return ApiResponse.<Object>builder()
                .data(response)
                .message("Plan generated successfully")
                .build();
    }

    /**
     * Judge cross-check - specialized endpoint for answer validation
     */
    @PostMapping("/judge")
    public ApiResponse<Map<String, Object>> judgeAnswer(
            @RequestParam String question,
            @RequestParam String answer,
            @RequestBody String sourceDocuments) {
        log.info("Judging answer for question: {}", question);
        
        Map<String, Object> context = new HashMap<>();
        context.put("generatedAnswer", answer);
        context.put("sourceDocuments", sourceDocuments);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.JUDGE_CROSS_CHECK)
                .userInput(question)
                .context(context)
                .build();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = generationService.generate(request, Map.class);
        
        return ApiResponse.<Map<String, Object>>builder()
                .data(result)
                .message("Judgment completed")
                .build();
    }

    /**
     * Interpret calculation - specialized endpoint for calculation interpretation
     */
    @PostMapping("/interpret-calculation")
    public ApiResponse<String> interpretCalculation(
            @RequestParam String question,
            @RequestParam String calculationResult) {
        log.info("Interpreting calculation for question: {}", question);
        
        Map<String, Object> context = new HashMap<>();
        context.put("calculationResult", calculationResult);
        
        GenerationRequest request = GenerationRequest.builder()
                .taskType(TaskType.INTERPRET_CALCULATION)
                .userInput(question)
                .context(context)
                .build();
        
        String response = generationService.generate(request);
        
        return ApiResponse.<String>builder()
                .data(response)
                .message("Interpretation completed successfully")
                .build();
    }
}
