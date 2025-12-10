package com.team14.chatbot.service.RagServiceImpl.query_processor;

import com.team14.chatbot.enums.QueryIntent;

/**
 * Record to hold routing result data
 */
public record RoutingResult(
        QueryIntent intent,
        String explanation
) {}