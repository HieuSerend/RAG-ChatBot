package com.team14.chatbot.service;


import com.google.genai.Chat;
import com.team14.chatbot.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QueryRetrievalService queryRetrievalService;

    private static final int K_TOKENS = 3;

    private static final String RAG_PROMPT_TEMPLATE = """
        Bạn là một trợ lý ảo hỗ trợ nhóm 14 trong môn Các Chuyên Đề trong Khoa Học Máy Tính, chuyên sâu về LLM (Large Language Models).  
        Nhiệm vụ của bạn là trả lời các câu hỏi liên quan đến dự án bài tập lớn dựa trên thông tin được cung cấp.  
        
        Hãy tuân theo các hướng dẫn sau:  
        1. Trả lời rõ ràng, chính xác, phù hợp với trình độ sinh viên.  
        2. Chỉ sử dụng thông tin liên quan đến CONTEXT, không suy đoán hay thêm thông tin quá nằm ngoài CONTEXT.  
        3. Nếu CONTEXT không đủ để trả lời, hãy nói rõ: "Tôi không có đủ thông tin để trả lời câu hỏi này."  
        4. Nếu câu trả lời gồm nhiều ý, hãy liệt kê theo đầu dòng để dễ đọc.  
        5. Giải thích các thuật ngữ chuyên môn nếu có thể, để sinh viên dễ hiểu.  
        
        CONTEXT:
        {context}
        
        Vui lòng trả lời câu hỏi sau của user dựa vào thông tin trên:
        """;


    public ChatResponse generateResponse(String userQuery) {
        // Use QueryRetrievalService for advanced retrieval pipeline
        com.team14.chatbot.dto.response.RetrievalResponse retrievalResponse = 
                queryRetrievalService.retrieveWithCrag(userQuery);
        
        List<Document> similarDocuments = retrievalResponse.getDocuments();
        
        // Handle CRAG evaluation result
        if (retrievalResponse.getCragEvaluation() != null) {
            com.team14.chatbot.dto.response.CragEvaluation crag = retrievalResponse.getCragEvaluation();
            System.out.println(">>> CRAG Evaluation: " + crag.getQuality() + " - " + crag.getAction());
            
            // If CRAG says documents are BAD, return message indicating no information found
            if (crag.getQuality() == com.team14.chatbot.dto.response.CragEvaluation.DocumentQuality.BAD) {
                return ChatResponse.builder()
                        .answer("Tôi không tìm thấy thông tin liên quan đến câu hỏi của bạn trong cơ sở dữ liệu.")
                        .build();
            }
        }

        System.out.println(">>> Similar documents: " + similarDocuments.size());

        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        context = """
        Nhóm 14 đang làm dự án môn học về LLM (Large Language Models).
        ---
        Dự án là một chatbot, sử dụng các công nghệ: Frontend React, Backend Java Spring Boot, Database PostgreSQL.
        ---
        Nhóm gồm 5 thành viên.
        ---
        Các thành viên gồm: Đạt, Đông, Lê Hiếu, Đào Hiếu, Huy
        ---
        Dự án đang triển khai sprint Core RAG Chatbot
        """;


        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(RAG_PROMPT_TEMPLATE);

        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("context", context
                )
        );

        Message userMessage = new UserMessage(userQuery);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

//        System.out.println(">>> System Message: " + systemMessage.getText());
        System.out.println(">>> User Message: " + userMessage.getText());
        System.out.println(">>> Prompt: " + prompt.getContents());

        return ChatResponse.builder()
                .answer(chatClient.prompt(prompt).call().content())
                .build();
    }


}
