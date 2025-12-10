package com.team14.chatbot.service.RagServiceImpl.query_processor;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class IntentPlan {
    private String intentDescription;
    private String taskType; // ví dụ: knowledge_query, calculation, coding, analysis, rewrite...
    private String requiredInputs;
    private String expectedOutputs;

    // Nếu dùng multi-query
    private List<SubQueryPlan> subQueries;
    private String fusionRule; // union, intersection, weighted merge, ranking...

    // Metadata cho virtual thread
    private boolean subQueriesRunnableInParallel;
}

