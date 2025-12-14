package com.team14.chatbot.service.RagModules;

import java.util.List;

import com.team14.chatbot.service.RagModules.pipeline.PipelinePlan;
import com.team14.chatbot.service.RagModules.query_processor.IntentTask;

public interface PlannerService {

    List<PipelinePlan> createPlans(List<IntentTask> tasks);

}
