package za.co.qsnext.employeemanagement.selfservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
import za.co.qsnext.employeemanagement.exception.ErrorResponse;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;
import za.co.qsnext.employeemanagement.timesheet.dto.CreateSelfServiceTimesheetRequest;
import za.co.qsnext.employeemanagement.timesheet.dto.CreateTimesheetEntryRequest;
import za.co.qsnext.employeemanagement.timesheet.dto.TimesheetResponse;

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

    @Operation(
            summary = "Get own employee profile",
            description = "Returns the employee profile belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee profile not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Get own leave requests",
            description = "Returns paginated leave requests belonging to the authenticated employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Leave requests retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee profile not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Get own attendance",
            description = "Returns paginated attendance records belonging to the authenticated employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Attendance retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee profile not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Clock in",
            description = "Clocks the authenticated employee in for the current working day."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Clock-in successful"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Clock-in business rule violation",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Clock out",
            description = "Clocks the authenticated employee out for the current working day."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Clock-out successful"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Clock-out business rule violation",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Get own timesheets",
            description = "Returns paginated timesheets belonging to the authenticated employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Timesheets retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee profile not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Create own timesheet",
            description = "Creates a new draft timesheet for the authenticated employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Timesheet created successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TimesheetResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee profile not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Timesheet business rule violation",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Add timesheet entry",
            description = "Adds a work entry to the authenticated employee's draft timesheet."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Timesheet entry created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Timesheet or employee not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Timesheet business rule violation",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Submit own timesheet",
            description = "Submits the authenticated employee's draft timesheet for approval."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Timesheet submitted successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TimesheetResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Timesheet or employee not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Timesheet business rule violation",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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