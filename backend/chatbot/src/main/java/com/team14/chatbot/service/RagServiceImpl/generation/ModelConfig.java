package com.team14.chatbot.service.RagServiceImpl.generation;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Configuration for LLM model selection and parameters
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelConfig {
    /**
     * Model name (e.g., "gemini-2.5-flash", "gpt-4-turbo")
     */
    String modelName;
    
    /**
     * Temperature for generation (0.0 = deterministic, 1.0 = creative)
     */
    Double temperature;
    
    /**
     * Maximum tokens to generate
     */
    Integer maxTokens;
    
    /**
     * Top-p sampling parameter
     */
    Double topP;
    
    public ModelConfig(String modelName, Double temperature) {
        this.modelName = modelName;
        this.temperature = temperature;
    }
}
