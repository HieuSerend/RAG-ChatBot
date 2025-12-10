package com.team14.chatbot.service.RagServiceImpl.orchestrator;

import com.team14.chatbot.service.RagServiceImpl.orchestrator.executor.PipelinePlan;

public interface PipelineExecutorService {

    String execute(PipelinePlan plan);

}
