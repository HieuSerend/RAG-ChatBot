package com.team14.chatbot.service.RagServiceImpl;

import com.team14.chatbot.service.RagServiceImpl.retriever.RetrievalRequest;
import com.team14.chatbot.service.RagServiceImpl.retriever.RetrievalResponse;

import java.util.Map;

public interface RetrievalService {
    RetrievalResponse retrieveDocuments(String userInput, Map<String, Object> filterMetadata);
}
