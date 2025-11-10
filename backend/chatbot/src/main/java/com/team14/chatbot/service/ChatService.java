package com.team14.chatbot.service;


import com.team14.chatbot.repository.HybridChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service

public class ChatService {
    private final ChatClient chatClient;
    private final VectorStore knowledgeBaseStore;
    private final HybridChatMemoryRepository hybridChatMemoryRepository;

    @Autowired
    public ChatService(
            @Qualifier("knowledgeBaseVectorStore") VectorStore knowledgeBaseStore,
            ChatClient chatClient,
            HybridChatMemoryRepository hybridChatMemoryRepository) {
        this.knowledgeBaseStore = knowledgeBaseStore;
        this.chatClient = chatClient;
        this.hybridChatMemoryRepository = hybridChatMemoryRepository;
    }

    private static final int K_TOKENS = 3;

    private static final String PROMPT_TEMPLATE = """
            [PHẦN HƯỚNG DẪN HỆ THỐNG - SYSTEMCONTEXT]
            Bạn là một Trợ lý Tài chính AI.
            Vai trò của bạn là cung cấp thông tin tài chính một cách chuyên nghiệp, khách quan và lịch sự.
            
            QUY TẮC TUYỆT ĐỐI:
            1.  **NGÔN NGỮ:** LUÔN LUÔN trả lời bằng tiếng Việt.
            
            2.  **GIỚI HẠN CHỦ ĐỀ:** Chỉ trả lời các câu hỏi liên quan trực tiếp đến tài chính (ngân hàng, đầu tư, thuế, bảo hiểm, v.v.).
                * Nếu người dùng hỏi về chủ đề không liên quan (ví dụ: thời tiết, thể thao, nấu ăn), hãy từ chối một cách lịch sự.
                * VÍ DỤ TỪ CHỐI: "Xin lỗi, tôi chỉ được huấn luyện để trả lời các câu hỏi về tài chính. Bạn có câu hỏi nào khác về chủ đề này không?"
            3.  **CẤM LỜI KHUYÊN TÀI CHÍNH:** TUYỆT ĐỐI KHÔNG đưa ra lời khuyên tài chính cá nhân, dự đoán thị trường, hoặc khuyến nghị mua/bán (ví dụ: "Bạn nên mua cổ phiếu X" hoặc "Tôi nghĩ thị trường sẽ tăng"). Bạn CHỈ cung cấp thông tin, dữ kiện và giải thích khái niệm.
            
            4.  **SỬ DỤNG LỊCH SỬ:** Tham khảo [CONVERSATIONCONTEXT] để hiểu bối cảnh và duy trì tính liên tục của cuộc trò chuyện (ví dụ: hiểu các đại từ như 'nó', 'cái đó').
            5.  **THÁI ĐỘ:** Luôn giữ giọng điệu chuyên nghiệp, lịch sự và hữu ích. Tránh sử dụng tiếng lóng.
            6.  **Trả lời từ kiến thức thực tế hoặc kiến thức từ [KNOWLEDGEBASECONTEXT], nếu kiến thức từ [KNOWLEDGEBASECONTEXT] là bổ ích
            ---
            [PHẦN NGỮ CẢNH TRI THỨC - KNOWLEDGEBASECONTEXT]
            Đây là các thông tin được trích xuất từ cơ sở kiến thức để giúp bạn trả lời:
            
            {KNOWLEDGE_BASE_CONTEXT}
            
            ---
            [PHẦN LỊCH SỬ HỘI THOẠI - CONVERSATIONCONTEXT]
            Đây là cuộc trò chuyện trước đó:
            
            {CONVERSATION_CONTEXT}
            
            ---
            [PHẦN CÂU HỎI CỦA NGƯỜI DÙNG - USERMESSAGE]
            Dựa vào tất cả thông tin ở trên, hãy trả lời câu hỏi sau:
            
            """;


    public Message oneTimeResponse(Prompt prompt) {
        return AssistantMessage.builder()
                .content(chatClient.prompt(prompt).call().content())
                .build();
    }

    public Prompt generatePrompt(UserMessage userMessage, String conversationId) {
        List<Document> similarDocuments = knowledgeBaseStore.
                similaritySearch(SearchRequest.builder()
                        .query(userMessage.getText())
                        .topK(K_TOKENS)
                        .build());

        System.out.println(">>> Similar documents: " + similarDocuments);

        String knowledgeBaseContext = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        List<Message> messageList = hybridChatMemoryRepository.findByConversationId(conversationId);
        String conversationContext = messageList.isEmpty() ? "" : messageList.toString();
        System.out.println("conversation context" + conversationContext);


        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(PROMPT_TEMPLATE);

        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("KNOWLEDGE_BASE_CONTEXT", knowledgeBaseContext,
                        "CONVERSATION_CONTEXT", conversationContext
                )
        );

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

//        System.out.println(">>> System Message: " + systemMessage.getText());
        System.out.println(">>> User Message: " + userMessage.getText());
        System.out.println(">>> Prompt: " + prompt.getContents());

        return prompt;
    }
}
