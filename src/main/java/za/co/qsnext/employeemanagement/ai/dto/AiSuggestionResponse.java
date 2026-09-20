package za.co.qsnext.employeemanagement.ai.dto;

import za.co.qsnext.employeemanagement.ai.AiSuggestion;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiSuggestionResponse(
        UUID id,
        String type,
        String subjectType,
        UUID subjectId,
        String providerName,
        String prompt,
        String responseText,
        OffsetDateTime createdAt
) {

    public static AiSuggestionResponse from(AiSuggestion suggestion) {
        return new AiSuggestionResponse(
                suggestion.getId(),
                suggestion.getType(),
                suggestion.getSubjectType(),
                suggestion.getSubjectId(),
                suggestion.getProviderName(),
                suggestion.getPrompt(),
                suggestion.getResponseText(),
                suggestion.getCreatedAt()
        );
    }
}
