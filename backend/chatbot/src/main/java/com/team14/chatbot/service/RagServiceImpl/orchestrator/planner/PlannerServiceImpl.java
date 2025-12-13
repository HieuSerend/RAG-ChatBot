package com.team14.chatbot.service.RagServiceImpl.orchestrator.planner;

import com.team14.chatbot.enums.QueryIntent;
import com.team14.chatbot.service.RagServiceImpl.orchestrator.PlannerService;
import com.team14.chatbot.service.RagServiceImpl.orchestrator.executor.PipelinePlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlannerServiceImpl implements PlannerService {

  @Override
  public PipelinePlan createPlan(QueryIntent intent, String userQuery) {
    try {
      return switch (intent) {
        case GREETING -> buildDirectPlan(intent, "Chào bạn, tôi có thể giúp gì?");
        case MALICIOUS_CONTENT -> buildDirectPlan(intent, "Tôi không thể trả lời nội dung này.");
        case CALCULATION -> buildCalculationPlan(intent, userQuery);
        case ADVISORY -> buildAdvisoryPlan(intent, userQuery);
        case KNOWLEDGE_QUERY, BEHAVIORAL -> buildKnowledgePlan(intent, userQuery);
        case UNCLEAR -> buildDirectPlan(intent, "Tôi chưa chắc ý bạn. Bạn mô tả rõ hơn nhé?");
        case NON_FINANCIAL -> buildDirectPlan(intent, "Tôi là chatbot tài chính, vui lòng hỏi về lĩnh vực tài chính.");
        default -> buildDirectPlan(intent, "Tôi chưa chắc ý bạn. Bạn mô tả rõ hơn nhé?");
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

  private PipelinePlan buildDirectPlan(QueryIntent intent, String response) {
    return PipelinePlan.builder()
        .intent(intent.name())
        .query(null)
        .directResponse(response)
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
            .intent(intent.name())
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
            .intent(intent.name())
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
            .intent(intent.name())
            .build())
        .build();
  }
}
