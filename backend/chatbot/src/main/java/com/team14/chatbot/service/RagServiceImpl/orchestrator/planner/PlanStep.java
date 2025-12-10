package com.team14.chatbot.service.RagServiceImpl.orchestrator.planner;

import com.team14.chatbot.enums.ToolType;
import com.team14.chatbot.service.RagServiceImpl.orchestrator.ModuleType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Builder
@Getter
public class PlanStep {
    // ID định danh (VD: "step_1")
    private String stepId;

    // Tên module cần gọi (Map từ JSON string sang Enum)
    private ModuleType module;

    // Dữ liệu đầu vào cho module đó (Thay vì "arguments")
    // VD: { "query": "...", "topK": 3 } hoặc { "expression": "..." }
    private Map<String, Object> args;

    // Mô tả ngắn gọn (Optional - để debug)
    private String description;
}

