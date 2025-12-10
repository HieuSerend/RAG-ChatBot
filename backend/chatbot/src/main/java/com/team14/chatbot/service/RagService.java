package com.team14.chatbot.service;

public interface RagService {
    String generate(String userQuery, String conversationContext);
}
