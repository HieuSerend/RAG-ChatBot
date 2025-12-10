package com.team14.chatbot.service.RagServiceImpl.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team14.chatbot.dto.request.GenerationRequest;
import com.team14.chatbot.dto.response.GenerationResponse;
import com.team14.chatbot.service.RagServiceImpl.GenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of GenerationService using Spring AI and Google Gemini.
 * 
 * This service orchestrates:
 * 1. Model routing based on task type
 * 2. Prompt template retrieval and rendering
 * 3. LLM invocation via Spring AI ChatModel
 * 4. Response parsing and type conversion
 */
@Service
// @RequiredArgsConstructor
@Slf4j
public class GenerationServiceImpl implements GenerationService {

    private final ChatClient geminiFlashClient;
    private final ChatClient geminiProClient;
    private final PromptRegistry promptRegistry;
    private final ModelRouter modelRouter;
    private final ObjectMapper objectMapper;

    public GenerationServiceImpl(
            @Qualifier("geminiFlashClient") ChatClient geminiFlashClient,
            @Qualifier("geminiProClient") ChatClient geminiProClient,
            PromptRegistry promptRegistry,
            ModelRouter modelRouter,
            ObjectMapper objectMapper) {
        this.geminiFlashClient = geminiFlashClient;
        this.geminiProClient = geminiProClient;
        this.promptRegistry = promptRegistry;
        this.modelRouter = modelRouter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(GenerationRequest request) {
        log.info("Generating content for task type: {}", request.getTaskType());

        try {
            // 1. Model Routing
            ModelConfig config = getModelConfig(request);
            log.debug("Using model: {} with temperature: {}", config.getModelName(), config.getTemperature());

            // 2. Prompt Engineering
            String systemPrompt = promptRegistry.getSystemPrompt(request.getTaskType());
            String userPrompt = promptRegistry.buildUserPrompt(
                    request.getTaskType(),
                    request.getUserInput(),
                    request.getContext());

            log.debug("System prompt length: {} chars", systemPrompt.length());
            log.debug("User prompt length: {} chars", userPrompt.length());

            // 3. Call LLM
            String response = callLlm(config, systemPrompt, userPrompt);

            log.info("Successfully generated content for task type: {}", request.getTaskType());
            return response;

        } catch (Exception e) {
            log.error("Error generating content for task type: {}", request.getTaskType(), e);
            throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T generate(GenerationRequest request, Class<T> responseType) {
        String rawResponse = generate(request);

        // If response type is String, return directly
        if (responseType.equals(String.class)) {
            return responseType.cast(rawResponse);
        }

        // Otherwise, parse JSON
        try {
            return parseJsonResponse(rawResponse, responseType);
        } catch (Exception e) {
            log.error("Error parsing response to type {}: {}", responseType.getName(), e.getMessage());
            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> GenerationResponse<T> generateWithMetadata(GenerationRequest request, Class<T> responseType) {
        ModelConfig config = getModelConfig(request);
        String rawResponse = generate(request);

        T parsedContent;
        if (responseType.equals(String.class)) {
            parsedContent = responseType.cast(rawResponse);
        } else {
            try {
                parsedContent = parseJsonResponse(rawResponse, responseType);
            } catch (Exception e) {
                log.error("Error parsing response to type {}: {}", responseType.getName(), e.getMessage());
                throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
            }
        }

        return GenerationResponse.<T>builder()
                .content(parsedContent)
                .modelUsed(config.getModelName())
                .temperature(config.getTemperature())
                .taskType(request.getTaskType().name())
                .rawResponse(rawResponse)
                .build();
    }

    /**
     * Get model configuration with overrides from request
     */
    private ModelConfig getModelConfig(GenerationRequest request) {
        return modelRouter.route(
                request.getTaskType(),
                request.getSpecificModel(),
                request.getTemperature());
    }

    /**
     * Call LLM using Spring AI ChatModel
     */
    private String callLlm(ModelConfig config, String systemPrompt, String userPrompt) {
        // Build messages
        Message systemMessage = new SystemMessage(systemPrompt);
        Message userMessage = new UserMessage(userPrompt);

        String modelName = config.getModelName();
        boolean isGemini = modelName == null || modelName.startsWith("gemini");

        ChatClient chatClient = selectChatClient(modelName);

        Prompt prompt;
        if (isGemini) {
            GoogleGenAiChatOptions chatOptions = GoogleGenAiChatOptions.builder()
                    .model(modelName != null ? modelName : "gemini-2.5-flash")
                    .temperature(config.getTemperature())
                    .build();
            prompt = new Prompt(List.of(systemMessage, userMessage), chatOptions);
        } else {
            prompt = new Prompt(List.of(systemMessage, userMessage));
        }

        String response = chatClient.prompt(prompt).call().content();

        log.debug("LLM response length: {} chars", response.length());
        return response;
    }

    private ChatClient selectChatClient(String modelName) {
        if (modelName == null || modelName.startsWith("gemini-2.5-flash")) {
            return geminiFlashClient;
        }
        if (modelName.startsWith("gemini-2.5-pro")) {
            return geminiProClient;
        }
        return geminiFlashClient;
    }

    /**
     * Parse JSON response from LLM
     */
    private <T> T parseJsonResponse(String rawResponse, Class<T> responseType) throws Exception {
        // Extract JSON from response (in case LLM adds markdown or extra text)
        String jsonContent = extractJson(rawResponse);

        // Parse using Jackson
        return objectMapper.readValue(jsonContent, responseType);
    }

    /**
     * Extract JSON content from response (removes markdown code blocks if present)
     */
    private String extractJson(String response) {
        // Try to find JSON in markdown code block
        Pattern jsonBlockPattern = Pattern.compile("```json\\s*\\n(.+?)\\n```", Pattern.DOTALL);
        Matcher matcher = jsonBlockPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Try to find JSON in generic code block
        Pattern codeBlockPattern = Pattern.compile("```\\s*\\n(.+?)\\n```", Pattern.DOTALL);
        matcher = codeBlockPattern.matcher(response);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            if (content.startsWith("{") || content.startsWith("[")) {
                return content;
            }
        }

        // Try to find JSON object or array in the text
        Pattern jsonPattern = Pattern.compile("(\\{.+?\\}|\\[.+?\\])", Pattern.DOTALL);
        matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // If no JSON markers found, assume entire response is JSON
        return response.trim();
    }
}
