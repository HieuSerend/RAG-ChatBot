package com.team14.chatbot.service.RagModules;

import com.team14.chatbot.service.RagModules.retriever.RetrievalResponse;

import java.util.Map;

public interface RetrievalService {
    RetrievalResponse retrieveDocuments(String userInput, Map<String, Object> filterMetadata);
}
