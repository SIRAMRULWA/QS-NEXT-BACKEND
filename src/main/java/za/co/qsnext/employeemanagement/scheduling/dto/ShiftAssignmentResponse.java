package za.co.qsnext.employeemanagement.scheduling.dto;

import za.co.qsnext.employeemanagement.scheduling.ShiftAssignment;

import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignmentResponse(
        UUID id,
        UUID employeeId,
        UUID shiftId,
        LocalDate workDate,
        String status
) {

    public static ShiftAssignmentResponse from(ShiftAssignment assignment) {
        return new ShiftAssignmentResponse(
                assignment.getId(),
                assignment.getEmployeeId(),
                assignment.getShiftId(),
                assignment.getWorkDate(),
                assignment.getStatus()
        );
    }
}
