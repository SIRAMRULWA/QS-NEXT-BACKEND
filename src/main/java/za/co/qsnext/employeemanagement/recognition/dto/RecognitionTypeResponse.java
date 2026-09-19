package za.co.qsnext.employeemanagement.recognition.dto;

import za.co.qsnext.employeemanagement.recognition.RecognitionType;

import java.util.UUID;

public record RecognitionTypeResponse(
        UUID id,
        String name,
        String description,
        int pointValue,
        boolean active
) {

    public static RecognitionTypeResponse from(RecognitionType type) {
        return new RecognitionTypeResponse(
                type.getId(), type.getName(), type.getDescription(), type.getPointValue(), type.isActive()
        );
    }
}
