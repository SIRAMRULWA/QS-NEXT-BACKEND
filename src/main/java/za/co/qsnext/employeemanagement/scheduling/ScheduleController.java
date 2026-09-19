package za.co.qsnext.employeemanagement.scheduling;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.scheduling.dto.AssignShiftRequest;
import za.co.qsnext.employeemanagement.scheduling.dto.CreateShiftRequest;
import za.co.qsnext.employeemanagement.scheduling.dto.ShiftAssignmentResponse;
import za.co.qsnext.employeemanagement.scheduling.dto.ShiftResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduling")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @PostMapping("/shifts")
    public ResponseEntity<ShiftResponse> createShift(
            @Valid @RequestBody CreateShiftRequest request
    ) {
        ShiftResponse response = scheduleService.createShift(
                request.name(), request.startTime(), request.endTime(), request.departmentId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('SCHEDULE_READ')")
    @GetMapping("/shifts")
    public ResponseEntity<List<ShiftResponse>> getAllShifts() {
        return ResponseEntity.ok(scheduleService.getAllShifts());
    }

    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @PostMapping("/assignments")
    public ResponseEntity<ShiftAssignmentResponse> assignShift(
            Authentication authentication,
            @Valid @RequestBody AssignShiftRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        ShiftAssignmentResponse response = scheduleService.assignShift(
                request.employeeId(), request.shiftId(), request.workDate(), userDetails.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> cancelAssignment(@PathVariable UUID assignmentId) {
        scheduleService.cancelAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<List<ShiftAssignmentResponse>> getEmployeeSchedule(
            @PathVariable UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(scheduleService.getEmployeeSchedule(employeeId, start, end));
    }

    @PreAuthorize("hasAuthority('SCHEDULE_READ')")
    @GetMapping("/my-schedule")
    public ResponseEntity<List<ShiftAssignmentResponse>> getOwnSchedule(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                scheduleService.getOwnSchedule(userDetails.getUserId(), start, end)
        );
    }
}
