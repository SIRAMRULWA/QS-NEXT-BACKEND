package za.co.qsnext.employeemanagement.performance.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePerformanceReviewRequest(

        @NotNull(message = "Cycle ID is required")
        UUID cycleId,

        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        /**
         * Optional - when omitted, the employee's manager (per the
         * Employee directory's manager hierarchy) is used as the reviewer.
         */
        UUID reviewerUserId
) {
}
