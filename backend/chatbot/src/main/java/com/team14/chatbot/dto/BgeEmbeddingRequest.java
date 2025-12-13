package com.team14.chatbot.dto;

import java.util.List;

// Class đại diện cho request gửi đi
public record BgeEmbeddingRequest(List<String> texts) {}