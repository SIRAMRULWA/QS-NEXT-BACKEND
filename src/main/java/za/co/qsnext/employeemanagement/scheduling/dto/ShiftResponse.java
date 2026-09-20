package za.co.qsnext.employeemanagement.scheduling.dto;

import za.co.qsnext.employeemanagement.scheduling.Shift;

import java.time.LocalTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        UUID departmentId
) {

    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getName(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDepartmentId()
        );
    }
}
