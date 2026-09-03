package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.leave.LeaveRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LeaveReportResponse(
        UUID employeeId,
        int year,
        int totalRequests,
        int approvedRequests,
        int pendingRequests,
        int rejectedRequests,
        int cancelledRequests,
        double totalApprovedDays,
        List<LeaveRequestSummary> requests
) {

    public static LeaveReportResponse from(
            UUID employeeId,
            int year,
            List<LeaveRequest> leaveRequests
    ) {

        int approved = 0;
        int pending = 0;
        int rejected = 0;
        int cancelled = 0;
        double approvedDays = 0;

        List<LeaveRequestSummary> requests =
                leaveRequests.stream()
                        .map(LeaveRequestSummary::from)
                        .toList();

        for (LeaveRequest request : leaveRequests) {

            switch (request.getStatus()) {

                case "APPROVED" -> {
                    approved++;
                    approvedDays += calculateDays(
                            request.getStartDate(),
                            request.getEndDate()
                    );
                }

                case "PENDING" -> pending++;

                case "REJECTED" -> rejected++;

                case "CANCELLED" -> cancelled++;

                default -> {
                    // Database CHECK constraint prevents unknown statuses.
                }
            }
        }

        return new LeaveReportResponse(
                employeeId,
                year,
                leaveRequests.size(),
                approved,
                pending,
                rejected,
                cancelled,
                approvedDays,
                requests
        );
    }

    private static long calculateDays(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return java.time.temporal.ChronoUnit.DAYS.between(
                startDate,
                endDate
        ) + 1;
    }

    public record LeaveRequestSummary(
            UUID id,
            String leaveType,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {

        public static LeaveRequestSummary from(
                LeaveRequest request
        ) {
            return new LeaveRequestSummary(
                    request.getId(),
                    request.getLeaveType(),
                    request.getStatus(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getReason()
            );
        }
    }
}