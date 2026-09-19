package za.co.qsnext.employeemanagement.performance.dto;

import za.co.qsnext.employeemanagement.performance.PerformanceCycle;

import java.time.LocalDate;
import java.util.UUID;

public record PerformanceCycleResponse(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {

    public static PerformanceCycleResponse from(PerformanceCycle cycle) {
        return new PerformanceCycleResponse(
                cycle.getId(), cycle.getName(), cycle.getStartDate(), cycle.getEndDate(), cycle.getStatus()
        );
    }
}
