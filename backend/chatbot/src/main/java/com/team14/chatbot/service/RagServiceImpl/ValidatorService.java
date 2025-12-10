package com.team14.chatbot.service.RagServiceImpl;

import com.team14.chatbot.service.RagServiceImpl.validator.ValidationResponse;
import org.springframework.ai.document.Document;

import java.util.List;

public interface ValidatorService {

    ValidationResponse validateInput(String rawQuery);

    ValidationResponse validateOutput(String generatedOutput, String userInput, List<Document> documents);
}