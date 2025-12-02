package com.team14.chatbot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for Query Processing Service.
 * Contains the raw user query and optional context for processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QueryProcessingRequest {
    
    /**
     * The raw query from the user
     */
    String query;
    
    /**
     * Optional conversation history for context-aware processing
     * Format: List of previous Q&A pairs as a single string
     */
    String conversationHistory;
    
    /**
     * Optional user ID for personalization
     */
    String userId;
    
    /**
     * Enable/disable step-back prompting transformation
     */
    @Builder.Default
    boolean enableStepBack = true;
    
    /**
     * Enable/disable HyDE (Hypothetical Document Embedding) transformation
     */
    @Builder.Default
    boolean enableHyde = true;
    
    /**
     * Enable/disable multi-query expansion
     */
    @Builder.Default
    boolean enableMultiQuery = true;
    
    /**
     * Number of expanded queries to generate (default: 3)
     */
    @Builder.Default
    int multiQueryCount = 3;
}

