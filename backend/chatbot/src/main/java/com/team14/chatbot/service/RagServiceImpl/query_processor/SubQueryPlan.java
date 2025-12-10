package com.team14.chatbot.service.RagServiceImpl.query_processor;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubQueryPlan {
    private String query;
    private String purpose;
}

