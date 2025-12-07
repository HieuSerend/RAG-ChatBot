package com.team14.chatbot.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Generic response wrapper for GenerationService results
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerationResponse<T> {
    /**
     * The generated content (can be String, JSON object, or custom type)
     */
    T content;
    
    /**
     * Model used for generation
     */
    String modelUsed;
    
    /**
     * Temperature used
     */
    Double temperature;
    
    /**
     * Task type that was executed
     */
    String taskType;
    
    /**
     * Raw response from LLM (for debugging)
     */
    String rawResponse;
}
