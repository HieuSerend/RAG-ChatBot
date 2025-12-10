package com.team14.chatbot.service.RagServiceImpl;

import com.team14.chatbot.repository.HybridChatMemoryRepository;
import com.team14.chatbot.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final OrchestratorService orchestratorService;



    @Override
    public String generate(String userQuery, String conversationContext) {
        return orchestratorService.handleUserRequest(userQuery, conversationContext);
    }
}
