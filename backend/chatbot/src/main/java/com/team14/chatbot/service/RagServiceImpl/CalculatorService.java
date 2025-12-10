package com.team14.chatbot.service.RagServiceImpl;

import com.team14.chatbot.service.RagServiceImpl.calculator.dto.CalculationRequest;
import com.team14.chatbot.service.RagServiceImpl.calculator.dto.CalculationResponse;

public interface CalculatorService {
    /**
     * Thực hiện tính toán biểu thức hoặc thống kê dữ liệu.
     * * @param request Chứa biểu thức (VD: "SUM(A)") và biến số (Variables).
     * @return Kết quả số học (BigDecimal) hoặc lỗi nếu có.
     */
    CalculationResponse calculate(CalculationRequest request);
}
