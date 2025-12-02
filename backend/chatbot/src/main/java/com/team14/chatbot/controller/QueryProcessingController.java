package com.team14.chatbot.controller;

import com.team14.chatbot.dto.request.QueryProcessingRequest;
import com.team14.chatbot.dto.response.ApiResponse;
import com.team14.chatbot.dto.response.QueryProcessingResponse;
import com.team14.chatbot.service.QueryProcessingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Query Processing operations.
 * Exposes endpoints for query routing, transformation, and expansion.
 */
@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@CrossOrigin(origins = "*")
public class QueryProcessingController {

    QueryProcessingService queryProcessingService;

    /**
     * Process a user query through the complete pipeline:
     * 1. Query Routing - Classify intent and decide pipeline
     * 2. Query Transformation - Apply step-back and HyDE
     * 3. Query Expansion - Generate multiple query variations
     *
     * @param request The query processing request containing the raw query and options
     * @return QueryProcessingResponse with routing decision, transformed queries, and expanded queries
     */
    @PostMapping("/process")
    public ApiResponse<QueryProcessingResponse> processQuery(@RequestBody QueryProcessingRequest request) {
        log.info("Received query processing request: {}", request.getQuery());
        
        QueryProcessingResponse response = queryProcessingService.processQuery(request);
        
        return ApiResponse.<QueryProcessingResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Quick route check - only performs intent classification without full processing.
     * Useful for deciding the pipeline before full processing.
     *
     * @param query The raw query string to classify
     * @return QueryProcessingResponse with only routing decision populated
     */
    @GetMapping("/route")
    public ApiResponse<QueryProcessingResponse> routeQuery(@RequestParam String query) {
        log.info("Received routing request for: {}", query);
        
        QueryProcessingRequest request = QueryProcessingRequest.builder()
                .query(query)
                .enableStepBack(false)
                .enableHyde(false)
                .enableMultiQuery(false)
                .build();
        
        QueryProcessingResponse response = queryProcessingService.processQuery(request);
        
        return ApiResponse.<QueryProcessingResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Transform a query using step-back prompting.
     * Returns the transformed query that captures the underlying concept.
     *
     * @param query The raw query to transform
     * @return QueryProcessingResponse with stepBackQuery populated
     */
    @GetMapping("/transform/step-back")
    public ApiResponse<QueryProcessingResponse> transformStepBack(@RequestParam String query) {
        log.info("Received step-back transformation request for: {}", query);
        
        QueryProcessingRequest request = QueryProcessingRequest.builder()
                .query(query)
                .enableStepBack(true)
                .enableHyde(false)
                .enableMultiQuery(false)
                .build();
        
        QueryProcessingResponse response = queryProcessingService.processQuery(request);
        
        return ApiResponse.<QueryProcessingResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Generate a hypothetical document using HyDE technique.
     * Returns a document that can be used for semantic search.
     *
     * @param query The query to generate hypothetical document for
     * @return QueryProcessingResponse with hypotheticalDocument populated
     */
    @GetMapping("/transform/hyde")
    public ApiResponse<QueryProcessingResponse> generateHyde(@RequestParam String query) {
        log.info("Received HyDE generation request for: {}", query);
        
        QueryProcessingRequest request = QueryProcessingRequest.builder()
                .query(query)
                .enableStepBack(false)
                .enableHyde(true)
                .enableMultiQuery(false)
                .build();
        
        QueryProcessingResponse response = queryProcessingService.processQuery(request);
        
        return ApiResponse.<QueryProcessingResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Expand a query into multiple variations.
     * Returns a list of semantically similar but differently phrased queries.
     *
     * @param query The query to expand
     * @param count Number of expanded queries to generate (default: 3)
     * @return QueryProcessingResponse with expandedQueries populated
     */
    @GetMapping("/expand")
    public ApiResponse<QueryProcessingResponse> expandQuery(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int count) {
        log.info("Received query expansion request for: {} (count: {})", query, count);
        
        QueryProcessingRequest request = QueryProcessingRequest.builder()
                .query(query)
                .enableStepBack(false)
                .enableHyde(false)
                .enableMultiQuery(true)
                .multiQueryCount(count)
                .build();
        
        QueryProcessingResponse response = queryProcessingService.processQuery(request);
        
        return ApiResponse.<QueryProcessingResponse>builder()
                .result(response)
                .build();
    }
}

