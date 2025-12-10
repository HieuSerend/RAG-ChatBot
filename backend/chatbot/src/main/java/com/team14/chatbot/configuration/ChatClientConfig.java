package com.team14.chatbot.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableScheduling
public class ChatClientConfig {

    private static final String GEMINI_FLASH = "gemini-2.5-flash";
    private static final String GEMINI_PRO = "gemini-2.5-pro";

    @Bean("geminiFlashClient")
    public ChatClient geminiFlashClient(GoogleGenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(GEMINI_FLASH)
                        .build())
                .build();
    }

    @Bean("geminiProClient")
    public ChatClient geminiProClient(GoogleGenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(GEMINI_PRO)
                        .build())
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    @Bean
    public OllamaChatModel ollamaLlamaModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("llama3.2:1b")
                        .build())
                .build();
    }

    @Bean("llama3_2_1b")
    public ChatClient llama3_2_1b(OllamaChatModel ollamaLlamaModel) {
        return ChatClient.create(ollamaLlamaModel);
    }
}