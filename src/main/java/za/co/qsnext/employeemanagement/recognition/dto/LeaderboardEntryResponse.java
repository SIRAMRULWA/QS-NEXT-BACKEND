package za.co.qsnext.employeemanagement.recognition.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(UUID employeeId, String employeeName, long totalPoints) {
}
