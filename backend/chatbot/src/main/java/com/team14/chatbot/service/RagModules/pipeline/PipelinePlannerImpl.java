package com.team14.chatbot.service.RagModules.pipeline;

import com.team14.chatbot.enums.QueryIntent;
import com.team14.chatbot.enums.TaskType;
import com.team14.chatbot.service.RagModules.PlannerService;
import com.team14.chatbot.service.RagModules.generation.Model;
import com.team14.chatbot.service.RagModules.query_processor.IntentTask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelinePlannerImpl implements PlannerService {

  @Override
  public List<PipelinePlan> createPlans(List<IntentTask> tasks) {
    return tasks
        .stream()
        .map(task -> createPlan(task.intent(), task.query()))
        .toList();
  }

  private PipelinePlan createPlan(QueryIntent intent, String userQuery) {
    try {
      return switch (intent) {
        case KNOWLEDGE_QUERY -> buildKnowledgePlan(intent, userQuery);
        case ADVISORY -> buildAdvisoryPlan(intent, userQuery);
        case CALCULATION -> buildCalculationPlan(intent, userQuery);
        case MALICIOUS_CONTENT -> {
          yield PipelinePlan.builder()
              .intent(intent.name())
              .query(userQuery)
              .directResponse("Phát hiện nội dung độc hại, vui lòng đặt lại câu hỏi khác.")
              .build();
        }
        case NON_FINANCIAL -> buildDirectPlan(intent, userQuery);
        default -> buildDirectPlan(intent, userQuery);
      };
    } catch (Exception e) {
      log.error("Planner error", e);
      return PipelinePlan.builder()
          .intent("ERROR")
          .query(userQuery)
          .directResponse("Xin lỗi, hệ thống đang bận. Vui lòng thử lại sau.")
          .build();
    }
  }

  private PipelinePlan buildDirectPlan(QueryIntent intent, String userQuery) {
    return PipelinePlan.builder()
        .intent(intent.name())
        .query(userQuery)
        .retrievalConfig(null)
        .generationConfig(null)
        .build();
  }

  private PipelinePlan buildKnowledgePlan(QueryIntent intent, String userQuery) {
    return PipelinePlan.builder()
        .intent(intent.name())
        .query(userQuery)
        // For knowledge queries: keep pipeline simple -> 1 retrieve, 1 gen, 1 validate
        .enableStepBack(false)
        .enableHyde(false)
        .retrievalConfig(PipelinePlan.RetrievalConfig.builder()
            .query(userQuery)
            .topK(5)
            .enableMultiQuery(false)
            .multiQueryCount(1)
            .build())
        .generationConfig(PipelinePlan.GenerationConfig.builder()
                .model(Model.GEMINI_2_5_FLASH)
            .build())
        .build();
  }

  private PipelinePlan buildAdvisoryPlan(QueryIntent intent, String userQuery) {
    return PipelinePlan.builder()
        .intent(intent.name())
        .query(userQuery)
        // Enable all advanced techniques for advisory queries
        .enableStepBack(true)
        .enableHyde(true)
        .retrievalConfig(PipelinePlan.RetrievalConfig.builder()
            .query(userQuery)
            .topK(5)
            .enableMultiQuery(true)
            .multiQueryCount(3)
            .build())
        .generationConfig(PipelinePlan.GenerationConfig.builder()
                .model(Model.GEMINI_2_5_FLASH)
            .build())
        .build();
  }

  private PipelinePlan buildCalculationPlan(QueryIntent intent, String userQuery) {
    return PipelinePlan.builder()
        .intent(intent.name())
        .query(userQuery)
        .enableStepBack(false)
        .enableHyde(false)
        .calculationConfig(PipelinePlan.CalculationConfig.builder()
            .expression(userQuery)
            .build())
        .generationConfig(PipelinePlan.GenerationConfig.builder()
                .model(Model.GEMINI_2_5_FLASH)
            .build())
        .build();
  }
}
