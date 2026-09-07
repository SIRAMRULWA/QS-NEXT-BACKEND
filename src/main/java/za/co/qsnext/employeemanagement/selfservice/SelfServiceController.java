package za.co.qsnext.employeemanagement.selfservice;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;
import za.co.qsnext.employeemanagement.timesheet.dto.CreateTimesheetEntryRequest;
import za.co.qsnext.employeemanagement.timesheet.dto.CreateSelfServiceTimesheetRequest;import za.co.qsnext.employeemanagement.timesheet.dto.TimesheetResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/self-service")
public class SelfServiceController {

    private final SelfServiceService selfServiceService;

    public SelfServiceController(
            SelfServiceService selfServiceService
    ) {
        this.selfServiceService = selfServiceService;
    }

    /*
     * ============================================================
     * PROFILE
     * ============================================================
     */

    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    @GetMapping("/profile")
    public ResponseEntity<SelfServiceProfileResponse> getOwnProfile(
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        SelfServiceProfileResponse response =
                selfServiceService.getOwnProfile(
                        userDetails.getUserId()
                );

        return ResponseEntity.ok(response);
    }

    /*
     * ============================================================
     * LEAVE
     * ============================================================
     */

    @PreAuthorize("hasAuthority('LEAVE_READ')")
    @GetMapping("/leave")
    public ResponseEntity<Page<LeaveResponse>> getOwnLeave(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        int safePage = Math.max(page, 0);

        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<LeaveResponse> response =
                selfServiceService.getOwnLeave(
                        userDetails.getUserId(),
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /*
     * ============================================================
     * ATTENDANCE
     * ============================================================
     */

    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    @GetMapping("/attendance")
    public ResponseEntity<Page<AttendanceResponse>> getOwnAttendance(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        int safePage = Math.max(page, 0);

        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "attendanceDate"
                )
        );

        return ResponseEntity.ok(
                selfServiceService.getOwnAttendance(
                        userDetails.getUserId(),
                        pageable
                )
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_CLOCK_IN')")
    @PostMapping("/attendance/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                selfServiceService.clockIn(
                        userDetails.getUserId()
                )
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_CLOCK_OUT')")
    @PostMapping("/attendance/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                selfServiceService.clockOut(
                        userDetails.getUserId()
                )
        );
    }

    /*
     * ============================================================
     * TIMESHEETS
     * ============================================================
     */

    @PreAuthorize("hasAuthority('TIMESHEET_READ')")
    @GetMapping("/timesheets")
    public ResponseEntity<Page<TimesheetResponse>> getOwnTimesheets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        int safePage = Math.max(page, 0);

        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "periodStart"
                )
        );

        return ResponseEntity.ok(
                selfServiceService.getOwnTimesheets(
                        userDetails.getUserId(),
                        pageable
                )
        );
    }

    @PreAuthorize("hasAuthority('TIMESHEET_CREATE')")
    @PostMapping("/timesheets")
    public ResponseEntity<TimesheetResponse> createTimesheet(
            Authentication authentication,
            @Valid @RequestBody CreateSelfServiceTimesheetRequest request
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        TimesheetResponse response =
                selfServiceService.createTimesheet(
                        userDetails.getUserId(),
                        request.periodStart(),
                        request.periodEnd()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasAuthority('TIMESHEET_ENTRY_CREATE')")
    @PostMapping("/timesheets/{timesheetId}/entries")
    public ResponseEntity<Void> addTimesheetEntry(
            Authentication authentication,
            @PathVariable UUID timesheetId,
            @Valid @RequestBody CreateTimesheetEntryRequest request
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        selfServiceService.addTimesheetEntry(
                userDetails.getUserId(),
                timesheetId,
                request.workDate(),
                request.hoursWorked(),
                request.description()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PreAuthorize("hasAuthority('TIMESHEET_SUBMIT')")
    @PostMapping("/timesheets/{timesheetId}/submit")
    public ResponseEntity<TimesheetResponse> submitTimesheet(
            Authentication authentication,
            @PathVariable UUID timesheetId
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        TimesheetResponse response =
                selfServiceService.submitTimesheet(
                        userDetails.getUserId(),
                        timesheetId
                );

        return ResponseEntity.ok(response);
    }
}