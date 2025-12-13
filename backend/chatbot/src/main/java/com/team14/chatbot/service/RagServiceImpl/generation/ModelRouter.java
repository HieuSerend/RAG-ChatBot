package com.team14.chatbot.service.RagServiceImpl.generation;

import com.team14.chatbot.enums.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Routes task types to appropriate model configurations.
 * Currently configured to use Gemini 2.5 Flash for all tasks with varying
 * temperatures.
 */
@Component
@Slf4j
public class ModelRouter {

    private static final String GEMINI_FLASH_MODEL = "gemini-2.5-flash";

    /**
     * Route task type to appropriate model configuration.
     * All tasks use Gemini 2.5 Flash with different temperature settings.
     * 
     * @param type The task type to route
     * @return ModelConfig with appropriate settings
     */
    public ModelConfig route(TaskType type) {
        ModelConfig config;

        switch (type) {
            case GENERATE_PLAN:
            case CALCULATION_PLANNING:
            case JUDGE_CROSS_CHECK:
                // Planning and judging tasks require more deterministic, logical output
                config = new ModelConfig(GEMINI_FLASH_MODEL, 0.2);
                log.debug("Routing {} to {} with temperature 0.2 (deterministic)", type, GEMINI_FLASH_MODEL);
                break;

            case ANALYZE_INTENT:
            case SAFETY_CHECK:
                // Intent analysis and safety checks need consistency
                config = new ModelConfig(GEMINI_FLASH_MODEL, 0.0);
                log.debug("Routing {} to {} with temperature 0.0 (strict)", type, GEMINI_FLASH_MODEL);
                break;

            case EXPLAIN_TERM:
            case SUMMARIZE_DOCS:
                // Explanation and summarization benefit from slight creativity
                config = new ModelConfig(GEMINI_FLASH_MODEL, 0.7);
                log.debug("Routing {} to {} with temperature 0.7 (creative)", type, GEMINI_FLASH_MODEL);
                break;

            case INTERPRET_CALCULATION:
                // Calculation interpretation needs balance
                config = new ModelConfig(GEMINI_FLASH_MODEL, 0.3);
                log.debug("Routing {} to {} with temperature 0.3 (balanced)", type, GEMINI_FLASH_MODEL);
                break;

            default:
                // Default: balanced temperature
                config = new ModelConfig(GEMINI_FLASH_MODEL, 0.5);
                log.debug("Routing {} to {} with temperature 0.5 (default)", type, GEMINI_FLASH_MODEL);
                break;
        }

        return config;
    }

    /**
     * Get model configuration with custom overrides
     */
    public ModelConfig route(TaskType type, String specificModel, Double temperature) {
        ModelConfig config = route(type);

        // Apply overrides if provided
        if (specificModel != null && !specificModel.isEmpty()) {
            log.info("Overriding model from {} to {}", config.getModelName(), specificModel);
            config.setModelName(specificModel);
        }

        if (temperature != null) {
            log.info("Overriding temperature from {} to {}", config.getTemperature(), temperature);
            config.setTemperature(temperature);
        }

        return config;
    }
}
