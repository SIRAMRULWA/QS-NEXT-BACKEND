package za.co.qsnext.employeemanagement.recognition.dto;

import za.co.qsnext.employeemanagement.recognition.Recognition;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecognitionResponse(
        UUID id,
        UUID typeId,
        UUID givenByUserId,
        UUID givenToEmployeeId,
        String message,
        int points,
        String visibility,
        OffsetDateTime createdAt
) {

    public static RecognitionResponse from(Recognition recognition) {
        return new RecognitionResponse(
                recognition.getId(), recognition.getTypeId(), recognition.getGivenByUserId(),
                recognition.getGivenToEmployeeId(), recognition.getMessage(), recognition.getPoints(),
                recognition.getVisibility(), recognition.getCreatedAt()
        );
    }
}
