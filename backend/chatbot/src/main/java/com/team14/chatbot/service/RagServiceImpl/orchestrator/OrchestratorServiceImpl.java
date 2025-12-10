package com.team14.chatbot.service.RagServiceImpl.orchestrator;

import com.team14.chatbot.dto.request.GenerationRequest;
import com.team14.chatbot.enums.QueryIntent;
import com.team14.chatbot.enums.TaskType;
import com.team14.chatbot.service.RagServiceImpl.GenerationService;
import com.team14.chatbot.service.RagServiceImpl.OrchestratorService;
import com.team14.chatbot.service.RagServiceImpl.ValidatorService;
import com.team14.chatbot.service.RagServiceImpl.orchestrator.executor.PipelinePlan;
import com.team14.chatbot.service.RagServiceImpl.query_processor.QueryProcessingService;
import com.team14.chatbot.service.RagServiceImpl.query_processor.RoutingResult;
import com.team14.chatbot.service.RagServiceImpl.validator.ValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorServiceImpl implements OrchestratorService {

    private final PipelineExecutorService pipelineExecutor;
    private final QueryProcessingService queryProcessor;
    private final PlannerService planner;
    private final GenerationService generationService;
    private final ValidatorService validatorService;

    public String handleUserRequest(String rawMessage, String conversationHistory) {
        log.info(">>> NEW REQUEST: {}", rawMessage);

        // B0: validate input
        ValidationResponse inputValidation = validatorService.validateInput(rawMessage);
        if (!inputValidation.isValid()) {
            return "Yêu cầu không hợp lệ: " + inputValidation.getReason();
        }

        // B1: phân loại intent
        RoutingResult routing = queryProcessor.analyzeIntent(rawMessage, conversationHistory);
        QueryIntent intent = routing.intent();
        log.info("Intent: {}", intent);

        // B2: toxic hoặc cấm
        if (intent == QueryIntent.MALICIOUS_CONTENT) {
            return "Tôi không thể trả lời câu hỏi này do chính sách an toàn.";
        }

        // B3: tạo plan cho intent (hiện tại 1 intent; có thể mở rộng multi-intent sau)
        List<PipelinePlan> plans = new ArrayList<>();
        plans.add(planner.createPlan(intent, rawMessage));
        log.info("Plans: {}", plans);

        // B4: thực thi song song từng intent pipeline
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = plans.stream()
                    .map(p -> executor.submit(() -> pipelineExecutor.execute(p)))
                    .toList();
            List<String> responses = new ArrayList<>();
            for (Future<String> f : futures) {
                try {
                    String r = f.get();
                    if (r != null && !r.isBlank()) {
                        responses.add(r);
                    }
                } catch (Exception e) {
                    log.error("Intent pipeline failed", e);
                }
            }

            if (responses.isEmpty())
                return "Không có phản hồi từ các pipeline.";
            String fused = responses.size() == 1
                    ? responses.get(0)
                    : fuseIntentResponses(rawMessage, responses);
            return validateAndRecover(fused, rawMessage, responses, intent);
        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            return "Xin lỗi, hệ thống gặp lỗi khi xử lý yêu cầu.";
        }
    }

    private String fuseIntentResponses(String originalQuery, List<String> parts) {
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            ctx.append("### INTENT ").append(i + 1).append("\n");
            ctx.append(parts.get(i)).append("\n\n");
        }

        Map<String, Object> context = new HashMap<>();
        context.put("answers", ctx.toString());

        GenerationRequest req = GenerationRequest.builder()
                .taskType(TaskType.FUSION)
                .userInput(originalQuery)
                .context(context)
                .build();

        return generationService.generate(req);
    }

    /**
     * Validate fused output; if fail, self-correct once, then retry regenerate
     * twice, else fallback.
     */
    private String validateAndRecover(String fused, String originalQuery, List<String> parts, QueryIntent intent) {
        if (intent == QueryIntent.GREETING
                || intent == QueryIntent.MALICIOUS_CONTENT
                || intent == QueryIntent.UNCLEAR) {
            return fused;
        }

        ValidationResponse vr = validatorService.validateOutput(fused, originalQuery, List.of());
        if (vr.isValid())
            return fused;

        // Self-correct once
        String candidate = regenerateFusion(originalQuery, parts, "self-correct", vr.getReason());
        vr = validatorService.validateOutput(candidate, originalQuery, List.of());
        if (vr.isValid())
            return candidate;

        // Retry regenerate up to 2 times
        for (int i = 1; i <= 2; i++) {
            candidate = regenerateFusion(originalQuery, parts, "retry_" + i, vr.getReason());
            vr = validatorService.validateOutput(candidate, originalQuery, List.of());
            if (vr.isValid())
                return candidate;
        }

        return "Xin lỗi, tôi không thể đảm bảo câu trả lời an toàn/đúng. Phản hồi tốt nhất hiện có:\n" + candidate;
    }

    private String regenerateFusion(String originalQuery, List<String> parts, String mode, String validatorReason) {
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            ctx.append("### INTENT ").append(i + 1).append("\n");
            ctx.append(parts.get(i)).append("\n\n");
        }

        Map<String, Object> context = new HashMap<>();
        context.put("answers", ctx.toString());
        context.put("validatorReason", validatorReason);
        context.put("mode", mode);

        TaskType task = TaskType.FUSION_SELF_CORRECT;
        GenerationRequest req = GenerationRequest.builder()
                .taskType(task)
                .userInput(originalQuery)
                .context(context)
                .build();

        return generationService.generate(req);
    }
}
