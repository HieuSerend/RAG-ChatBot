package com.team14.chatbot.service.RagServiceImpl.orchestrator.executor;

import com.team14.chatbot.dto.request.GenerationRequest;
import com.team14.chatbot.enums.TaskType;
import com.team14.chatbot.service.RagServiceImpl.CalculatorService;
import com.team14.chatbot.service.RagServiceImpl.GenerationService;
import com.team14.chatbot.service.RagServiceImpl.calculator.dto.CalculationRequest;
import com.team14.chatbot.service.RagServiceImpl.calculator.dto.CalculationResponse;
import com.team14.chatbot.service.RagServiceImpl.orchestrator.PipelineExecutorService;
import com.team14.chatbot.service.RagServiceImpl.query_processor.QueryProcessingService;
import com.team14.chatbot.service.RagServiceImpl.retriever.QueryRetrievalService;
import com.team14.chatbot.service.RagServiceImpl.retriever.RetrievalRequest;
import com.team14.chatbot.service.RagServiceImpl.retriever.RetrievalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineExecutorImpl implements PipelineExecutorService {

    // private final QueryRetrievalService retrievalService;
    private final CalculatorService calculatorService;
    private final GenerationService generationService;
    private final QueryRetrievalService queryRetrievalService;
    private final QueryProcessingService queryProcessingService;

    private static final String FUSION_PROMPT = """
            Bạn là hệ thống tổng hợp câu trả lời.
            Câu hỏi gốc: "%s"
            Các câu trả lời thành phần (từ nhiều sub-query):
            %s
            Nhiệm vụ:
            - Hợp nhất thông tin, loại bỏ trùng lặp
            - Giải quyết mâu thuẫn (nếu có), chọn thông tin nhất quán hơn
            - Trả lời ngắn gọn, chính xác, mạch lạc
            Trả lời bằng tiếng Việt, súc tích.
            """;

    /**
     * Chạy Pipeline theo luồng cố định: Retrieve -> Calculate -> Generate
     */
    @Override
    public String execute(PipelinePlan plan) {

        // 0. Check Fast Path
        if (plan.getDirectResponse() != null) {
            return plan.getDirectResponse();
        }

        // intentPipeline: step_back + hyde + multiquery + SubQueryPipeline
        String pipelineQuery = plan.getQuery();
        String hydeDoc = pipelineQuery;

        if (plan.isEnableStepBack() && pipelineQuery != null) {
            pipelineQuery = queryProcessingService.transformWithStepBack(pipelineQuery);
        }
        log.info("PipelineQuery: {}", pipelineQuery);

        if (plan.isEnableHyde() && pipelineQuery != null) {
            hydeDoc = queryProcessingService.generateHypotheticalDocument(pipelineQuery);
        }
        log.info("HydeDoc: {}", hydeDoc);

        List<String> queries = buildRetrievalQueries(plan, hydeDoc, pipelineQuery);
        log.info("Queries: {}", queries);
        List<String> subResults = runSubQueryPipelines(plan, queries);
        log.info("SubResults: {}", subResults);
        if (subResults.isEmpty()) {
            return "Không có nội dung từ subquery.";
        }
        if (subResults.size() == 1) {
            return subResults.get(0);
        }
        return fuseSubQueryResponses(plan.getQuery(), subResults, plan);
    }

    private List<String> buildRetrievalQueries(PipelinePlan plan, String hydeDoc, String pipelineQuery) {
        if (plan.getRetrievalConfig() == null) {
            return List.of(pipelineQuery);
        }
        var config = plan.getRetrievalConfig();
        String retrievalQuery = config.getQuery() != null ? config.getQuery() : hydeDoc;
        if (retrievalQuery == null) {
            retrievalQuery = pipelineQuery;
        }
        List<String> queries = new ArrayList<>();
        if (retrievalQuery != null && !retrievalQuery.isBlank()) {
            queries.add(retrievalQuery.trim());
        }
        if (config.isEnableMultiQuery() && retrievalQuery != null && !retrievalQuery.isBlank()) {
            List<String> expanded = queryProcessingService.expandQueryForExecutor(retrievalQuery,
                    Math.max(2, config.getMultiQueryCount()));
            for (String q : expanded) {
                if (q != null && !q.isBlank()) {
                    queries.add(q.trim());
                }
            }
        }
        return queries.stream().distinct().toList();
    }

    /**
     * SubQueryPipeline: mỗi sub-query chạy Retrieval -> Calculation -> Generation,
     * song song.
     */
    private List<String> runSubQueryPipelines(PipelinePlan plan, List<String> queries) {
        if (queries.isEmpty())
            return List.of();

        var generationConfig = plan.getGenerationConfig();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = queries.stream()
                    .map(q -> executor.submit(() -> runSingleSubPipeline(plan, q, generationConfig)))
                    .toList();
            List<String> results = new ArrayList<>();
            for (Future<String> f : futures) {
                try {
                    String res = f.get();
                    if (res != null && !res.isBlank()) {
                        results.add(res);
                    }
                } catch (Exception e) {
                    log.error("SubQueryPipeline failed", e);
                }
            }
            return results;
        }
    }

    private String runSingleSubPipeline(PipelinePlan plan, String query,
            PipelinePlan.GenerationConfig generationConfig) {
        StringBuilder ctx = new StringBuilder();

        // Retrieval
        if (plan.getRetrievalConfig() != null) {
            var rCfg = plan.getRetrievalConfig();
            RetrievalRequest req = RetrievalRequest.builder()
                    .query(query)
                    .topK(rCfg.getTopK())
                    .build();
            RetrievalResponse docs = queryRetrievalService.retrieveDocuments(req.getQuery(), null);
            if (!docs.getDocuments().isEmpty()) {
                ctx.append("=== THÔNG TIN TÌM KIẾM ===\n");
                ctx.append("[Query] ").append(query).append("\n");
                String docsStr = docs.getDocuments().stream()
                        .map(d -> "- " + d.getFormattedContent())
                        .collect(Collectors.joining("\n"));
                ctx.append(docsStr).append("\n\n");
            }
        }

        // Calculation
        if (plan.getCalculationConfig() != null) {
            var cCfg = plan.getCalculationConfig();
            CalculationRequest req = CalculationRequest.builder()
                    .expression(cCfg.getExpression())
                    .build();
            CalculationResponse result = calculatorService.calculate(req);
            if (result.isSuccess()) {
                ctx.append("=== KẾT QUẢ TÍNH TOÁN ===\n");
                ctx.append("Công thức: ").append(cCfg.getExpression()).append("\n");
                ctx.append("Giá trị: ").append(result.getValue()).append("\n\n");
            } else {
                ctx.append("=== LỖI TÍNH TOÁN ===\n");
                ctx.append("Loi tinh toan").append("\n\n");
            }
        }

        // Generation
        if (generationConfig != null) {
            String finalContext = ctx.toString();
            if (finalContext.isEmpty()) {
                finalContext = "Không có dữ liệu bổ sung. Hãy trả lời dựa trên kiến thức của bạn.";
            }
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("documents", finalContext);
            ctxMap.put("conversationHistory", "");
            GenerationRequest req = GenerationRequest.builder()
                    .taskType(TaskType.SUMMARIZE_DOCS)
                    .userInput(query)
                    .context(ctxMap)
                    .specificModel(generationConfig.getModel())
                    .build();
            return generationService.generate(req);
        }

        return ctx.toString();
    }

    private String fuseSubQueryResponses(String originalQuery, List<String> parts, PipelinePlan plan) {
        if (parts.isEmpty())
            return "Không có thông tin để tổng hợp.";
        if (parts.size() == 1)
            return parts.get(0);

        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            ctx.append("### SUBQUERY ").append(i + 1).append("\n");
            ctx.append(parts.get(i)).append("\n\n");
        }

        // Dùng LLM để fusion qua GenerationService (prompt registry)
        Map<String, Object> fusionCtx = new HashMap<>();
        fusionCtx.put("answers", ctx.toString());
        GenerationRequest fusionReq = GenerationRequest.builder()
                .taskType(TaskType.FUSION)
                .userInput(originalQuery)
                .context(fusionCtx)
                .build();
        return generationService.generate(fusionReq);
    }
}
