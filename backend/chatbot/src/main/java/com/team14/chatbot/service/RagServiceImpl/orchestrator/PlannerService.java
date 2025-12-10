package com.team14.chatbot.service.RagServiceImpl.orchestrator;

import com.team14.chatbot.enums.QueryIntent;
import com.team14.chatbot.service.RagServiceImpl.orchestrator.executor.PipelinePlan;

public interface PlannerService {
    PipelinePlan createPlan(QueryIntent intent, String userQuery);
}
