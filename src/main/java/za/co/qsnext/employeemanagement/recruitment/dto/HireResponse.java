package za.co.qsnext.employeemanagement.recruitment.dto;

import java.util.UUID;

public record HireResponse(UUID employeeId, UUID userId, boolean onboardingStarted) {
}
