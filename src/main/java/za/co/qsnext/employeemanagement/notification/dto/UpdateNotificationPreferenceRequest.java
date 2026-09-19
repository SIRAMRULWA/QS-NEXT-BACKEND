package za.co.qsnext.employeemanagement.notification.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(

        @NotNull(message = "inAppEnabled is required")
        Boolean inAppEnabled,

        @NotNull(message = "emailEnabled is required")
        Boolean emailEnabled
) {
}
