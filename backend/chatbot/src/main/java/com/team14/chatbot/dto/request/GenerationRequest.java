package com.team14.chatbot.dto.request;

import com.team14.chatbot.enums.TaskType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for GenerationService.
 * Encapsulates all information needed for AI generation tasks.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerationRequest {
    /**
     * Type of task to perform (determines which prompt template and model config to use)
     */
    TaskType taskType;
    
    /**
     * User input text (query, question, etc.)
     */
    String userInput;
    
    /**
     * Additional context data for prompt template rendering
     * Can include: retrieved documents, conversation history, metadata, etc.
     */
    @Builder.Default
    Map<String, Object> context = new HashMap<>();
    
    /**
     * Optional: Override the default model selection for this task
     * If null, ModelRouter will choose the appropriate model based on taskType
     */
    String specificModel;
    
    /**
     * Optional: Temperature setting (0.0 - 1.0)
     * If null, uses default from ModelRouter
     */
    Double temperature;
}
