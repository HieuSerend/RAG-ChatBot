package com.team14.chatbot.service.RagModules.pipeline;

import com.team14.chatbot.service.RagModules.generation.GenerationRequest;
import com.team14.chatbot.service.RagModules.CalculatorService;
import com.team14.chatbot.service.RagModules.FusionService;
import com.team14.chatbot.service.RagModules.GenerationService;
import com.team14.chatbot.service.RagModules.calculator.CalculationResult;
import com.team14.chatbot.service.RagModules.PipelineExecutorService;
import com.team14.chatbot.service.RagModules.query_processor.QueryProcessingService;
import com.team14.chatbot.service.RagModules.retriever.QueryRetrievalService;
import com.team14.chatbot.service.RagModules.retriever.RetrievalRequest;
import com.team14.chatbot.service.RagModules.retriever.RetrievalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
    private final FusionService fusionService;
    private final QueryRetrievalService queryRetrievalService;
    private final QueryProcessingService queryProcessingService;

    // Prompt templates
    private static final String CALCULATION_PLANNING_PROMPT = """
            Extract mathematical expression from financial query. Return JSON with expression and reasoning steps.

            Query: "{query}"

            Return JSON only:
            {"expression": "valid math expression", "reasoningSteps": ["step1", "step2"]}
            """;

    private static final String INTERPRET_CALCULATION_PROMPT = """
            Explain calculation results using knowledge base context. Use natural, easy-to-understand language.

            Query: "{query}"
            Knowledge base:
            {documents}
            Calculation analysis:
            {calculationReasoning}

            Return explanation in Vietnamese only.
            """;

    private static final String SUMMARIZE_DOCS_PROMPT = """
            You are a professional financial assistant.

            Your tasks:
            - Answer the user's question using ONLY the provided knowledge base.
            - Explain the answer clearly in simple terms.
            - If applicable, provide one or more concrete examples to help understanding.
            - Do NOT infer, assume, or add information that is not present in the knowledge base.
            - If the knowledge base does not contain enough information, clearly state that you do not have sufficient data to answer.

            User query:
            "{query}"

            Knowledge base:
            {documents}

            Rules:
            - Respond in Vietnamese only.
            - Use clear explanations.
            - Examples must be based strictly on the knowledge base.
            """;

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
                    Math.max(3, config.getMultiQueryCount()));
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
        if (queries.isEmpty()) {
            log.warn("No queries to process in parallel");
            return List.of();
        }

        log.info("=== Starting parallel sub-query pipelines ===");
        log.info("Total queries to process: {}", queries.size());
        for (int i = 0; i < queries.size(); i++) {
            log.debug("Query {}: {}", i + 1, queries.get(i));
        }

        var generationConfig = plan.getGenerationConfig();
        long startTime = System.currentTimeMillis();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit all tasks
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < queries.size(); i++) {
                final int queryIndex = i;
                final String query = queries.get(i);
                log.debug("Submitting sub-query pipeline {}: {}", queryIndex + 1, query);
                Future<String> future = executor.submit(() -> {
                    long queryStartTime = System.currentTimeMillis();
                    log.info("[SubQuery {}] Starting pipeline execution for: {}", queryIndex + 1, query);
                    try {
                        String result = runSingleSubPipeline(plan, query, generationConfig);
                        long queryDuration = System.currentTimeMillis() - queryStartTime;
                        log.info("[SubQuery {}] Completed in {}ms | Query: {} | Result length: {}",
                                queryIndex + 1, queryDuration, query,
                                result != null ? result.length() : 0);
                        return result;
                    } catch (Exception e) {
                        long queryDuration = System.currentTimeMillis() - queryStartTime;
                        log.error("[SubQuery {}] Failed after {}ms | Query: {}",
                                queryIndex + 1, queryDuration, query, e);
                        throw e;
                    }
                });
                futures.add(future);
            }

            log.info("All {} sub-query pipelines submitted, waiting for completion...", futures.size());

            // Collect results
            List<String> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                Future<String> f = futures.get(i);
                try {
                    String res = f.get();
                    if (res != null && !res.isBlank()) {
                        results.add(res);
                        log.debug("[SubQuery {}] Result collected successfully, length: {}", i + 1, res.length());
                    } else {
                        log.warn("[SubQuery {}] Result is null or empty", i + 1);
                    }
                } catch (Exception e) {
                    log.error("[SubQuery {}] Failed to get result", i + 1, e);
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("=== Parallel sub-query pipelines completed ===");
            log.info("Total duration: {}ms | Queries processed: {}/{} | Successful results: {}",
                    totalDuration, results.size(), queries.size(), results.size());

            return results;
        }
    }

    private String runSingleSubPipeline(PipelinePlan plan, String query,
            PipelinePlan.GenerationConfig generationConfig) {
        StringBuilder ctx = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        long stepStartTime;

        // Retrieval
        if (plan.getRetrievalConfig() != null) {
            stepStartTime = System.currentTimeMillis();
            var rCfg = plan.getRetrievalConfig();
            log.debug("[SubQuery] Starting retrieval for: {} | topK: {}", query, rCfg.getTopK());

            RetrievalRequest req = RetrievalRequest.builder()
                    .query(query)
                    .topK(rCfg.getTopK())
                    .build();
            RetrievalResponse docs = queryRetrievalService.retrieveDocuments(req.getQuery(), null);

            long retrievalDuration = System.currentTimeMillis() - stepStartTime;
            if (!docs.getDocuments().isEmpty()) {
                ctx.append("=== KNOWLEDGE BASE ===\n");
                ctx.append("[Query] ").append(query).append("\n");
                String docsStr = docs.getDocuments().stream()
                        .map(d -> "- " + d.getText())
                        .collect(Collectors.joining("\n"));
                ctx.append(docsStr).append("\n\n");
                log.debug("[SubQuery] Retrieval completed in {}ms | Documents found: {} | Query: {}",
                        retrievalDuration, docs.getDocuments().size(), query);
            } else {
                log.debug("[SubQuery] Retrieval completed in {}ms | No documents found | Query: {}",
                        retrievalDuration, query);
            }
        }

        // Calculation
        if (plan.getCalculationConfig() != null) {
            stepStartTime = System.currentTimeMillis();
            var cCfg = plan.getCalculationConfig();
            String expression = cCfg.getExpression();
            log.debug("[SubQuery] Starting calculation step | Query: {} | Expression provided: {}",
                    query, expression != null && !expression.isBlank());

            try {
                // If no expression provided, try to extract it from the query
                if (expression == null || expression.isBlank()) {
                    log.debug("[SubQuery] No expression provided, extracting from query using LLM");
                    long planningStartTime = System.currentTimeMillis();

                    // Create prompt for calculation planning
                    PromptTemplate planningTemplate = new PromptTemplate(CALCULATION_PLANNING_PROMPT);
                    Map<String, Object> planningVars = Map.of("query", query);
                    Prompt planningPrompt = planningTemplate.create(planningVars);

                    GenerationRequest planReq = GenerationRequest.builder()
                            .prompt(planningPrompt)
                            .specificModel(generationConfig != null ? generationConfig.getModel() : null)
                            .build();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> analysisResult = generationService.generate(planReq, Map.class);
                    long planningDuration = System.currentTimeMillis() - planningStartTime;
                    log.debug("[SubQuery] Calculation planning completed in {}ms | Result: {}",
                            planningDuration, analysisResult);

                    if (analysisResult != null) {
                        String extractedExpr = (String) analysisResult.get("expression");
                        if (extractedExpr != null && !extractedExpr.isBlank()) {
                            expression = extractedExpr;

                            // Add reasoning steps if available
                            if (analysisResult.containsKey("reasoningSteps")) {
                                @SuppressWarnings("unchecked")
                                List<String> steps = (List<String>) analysisResult.get("reasoningSteps");
                                if (steps != null && !steps.isEmpty()) {
                                    reasoning.append("Problem analysis:\n");
                                    reasoning.append(String.join("\n", steps)).append("\n");
                                }
                            }
                        }
                    }
                }

                // If we have a valid expression, perform the calculation
                if (expression != null && !expression.isBlank()) {
                    ctx.append("=== CALCULATION ANALYSIS ===\n");
                    ctx.append("Query: ").append(query).append("\n");
                    ctx.append("Expression: ").append(expression).append("\n\n");

                    long calcStartTime = System.currentTimeMillis();
                    try {
                        // Call calculator service with the expression string directly
                        log.debug("[SubQuery] Evaluating expression: {}", expression);
                        CalculationResult result = calculatorService.calculate(expression);
                        long calcDuration = System.currentTimeMillis() - calcStartTime;

                        if (result.isSuccess()) {
                            ctx.append("=== CALCULATION RESULT ===\n");
                            ctx.append("Expression: ").append(expression).append("\n");
                            ctx.append("Result: ").append(result.getValue()).append("\n\n");

                            // Add to reasoning if not already added
                            if (reasoning.length() == 0) {
                                reasoning.append("1. Query analysis: ").append(query).append("\n");
                                reasoning.append("2. Mathematical expression: ").append(expression).append("\n");
                            }
                            reasoning.append("3. Calculation result: ").append(result.getValue()).append("\n");
                            log.debug("[SubQuery] Calculation successful in {}ms | Expression: {} | Result: {}",
                                    calcDuration, expression, result.getValue());
                        } else {
                            ctx.append("=== CALCULATION ERROR ===\n");
                            ctx.append("Cannot evaluate expression: ").append(expression).append("\n\n");
                            reasoning.append("Error: Cannot evaluate expression: ").append(expression).append("\n");
                            log.warn("[SubQuery] Calculation failed in {}ms | Expression: {}", calcDuration,
                                    expression);
                        }
                    } catch (Exception e) {
                        long calcDuration = System.currentTimeMillis() - calcStartTime;
                        log.error("[SubQuery] Calculation exception in {}ms | Expression: {}", calcDuration, expression,
                                e);
                        ctx.append("=== CALCULATION EXECUTION ERROR ===\n");
                        ctx.append("Error occurred: ").append(e.getMessage()).append("\n\n");
                        reasoning.append("Error: ").append(e.getMessage()).append("\n");
                    }
                } else {
                    ctx.append("=== CANNOT DETERMINE EXPRESSION ===\n\n");
                    reasoning.append("Cannot determine calculation expression from query.\n");
                    log.warn("[SubQuery] Cannot determine calculation expression from query");
                }

                long calculationTotalDuration = System.currentTimeMillis() - stepStartTime;
                log.debug("[SubQuery] Calculation step completed in {}ms | Query: {}", calculationTotalDuration, query);
            } catch (Exception e) {
                long calculationTotalDuration = System.currentTimeMillis() - stepStartTime;
                log.error("[SubQuery] Calculation planning error in {}ms | Query: {}", calculationTotalDuration, query,
                        e);
                ctx.append("=== CALCULATION PLANNING ERROR ===\n");
                ctx.append("Error analyzing query: ").append(e.getMessage()).append("\n\n");
                reasoning.append("Analysis error: ").append(e.getMessage()).append("\n");
            }
        }

        // Generation
        if (generationConfig != null) {
            stepStartTime = System.currentTimeMillis();
            log.debug("[SubQuery] Starting generation step | Query: {} | Model: {}",
                    query, generationConfig.getModel());

            String finalContext = ctx.toString();
            if (finalContext.isEmpty()) {
                finalContext = "No additional data. Answer based on your knowledge.";
                log.debug("[SubQuery] No context from retrieval/calculation, using default");
            } else {
                log.debug("[SubQuery] Context length: {} chars", finalContext.length());
            }

            // Build prompt based on whether we have calculation or not
            PromptTemplate promptTemplate;
            Map<String, Object> promptVars = new HashMap<>();
            promptVars.put("query", query);
            promptVars.put("documents", finalContext);

            boolean hasCalculation = plan.getCalculationConfig() != null && reasoning.length() > 0;
            if (hasCalculation) {
                // Use INTERPRET_CALCULATION prompt
                promptTemplate = new PromptTemplate(INTERPRET_CALCULATION_PROMPT);
                promptVars.put("calculationReasoning", reasoning.toString());
                log.debug("[SubQuery] Using INTERPRET_CALCULATION prompt | Reasoning length: {}", reasoning.length());
            } else {
                // Use SUMMARIZE_DOCS prompt
                promptTemplate = new PromptTemplate(SUMMARIZE_DOCS_PROMPT);
                log.debug("[SubQuery] Using SUMMARIZE_DOCS prompt");
            }

            Prompt prompt = promptTemplate.create(promptVars);

            GenerationRequest genReq = GenerationRequest.builder()
                    .prompt(prompt)
                    .specificModel(generationConfig.getModel())
                    .temperature(generationConfig.getTemperature())
                    .build();

            String result = generationService.generate(genReq);
            long generationDuration = System.currentTimeMillis() - stepStartTime;
            log.debug("[SubQuery] Generation completed in {}ms | Query: {} | Result length: {}",
                    generationDuration, query, result != null ? result.length() : 0);

            return result;
        }

        log.debug("[SubQuery] No generation config, returning context only | Query: {}", query);
        return ctx.toString();
    }

    private String fuseSubQueryResponses(String originalQuery, List<String> parts, PipelinePlan plan) {
        var model = plan.getGenerationConfig() != null ? plan.getGenerationConfig().getModel() : null;
        return fusionService.fuse(originalQuery, parts, model);
    }

}
