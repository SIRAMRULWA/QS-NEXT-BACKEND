package za.co.qsnext.employeemanagement.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.attendance.dto.ClockInRequest;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Attendance", description = "Clock in/out, attendance records and corrections.")
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService
    ) {
        this.attendanceService = attendanceService;
    }

    @PreAuthorize(
            "hasAuthority('ATTENDANCE_READ') and " +
                    "@attendanceAuthorizationService.canReadRecord(#attendanceId, authentication)"
    )
    @Operation(summary = "Get by id")
    @GetMapping("/{attendanceId}")
    public ResponseEntity<AttendanceResponse> getById(
            @PathVariable UUID attendanceId
    ) {

        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.getById(attendanceId)
                )
        );
    }

    @PreAuthorize(
            "hasAuthority('ATTENDANCE_READ') and " +
                    "@employeeAuthorizationService.canRead(#employeeId, authentication)"
    )
    @Operation(summary = "Get by employee")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<AttendanceResponse>> getByEmployee(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                attendanceService
                        .getByEmployee(employeeId, pageable)
                        .map(AttendanceResponse::from)
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    @Operation(summary = "Create")
    @PostMapping
    public ResponseEntity<AttendanceResponse> create(
            @Valid @RequestBody ClockInRequest request
    ) {

        Attendance attendance =
                attendanceService.create(
                        request.employeeId(),
                        request.attendanceDate()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        AttendanceResponse.from(attendance)
                );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_CLOCK_IN')")
    @Operation(summary = "Clock in")
    @PostMapping("/{attendanceId}/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(
            @PathVariable UUID attendanceId
    ) {

        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.clockIn(
                                attendanceId
                        )
                )
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_CLOCK_OUT')")
    @Operation(summary = "Clock out")
    @PostMapping("/{attendanceId}/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(
            @PathVariable UUID attendanceId
    ) {

        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.clockOut(
                                attendanceId
                        )
                )
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_MARK_ABSENT')")
    @Operation(summary = "Mark absent")
    @PatchMapping("/{attendanceId}/absent")
    public ResponseEntity<AttendanceResponse> markAbsent(
            @PathVariable UUID attendanceId
    ) {

        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.markAbsent(
                                attendanceId
                        )
                )
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_MARK_LATE')")
    @Operation(summary = "Mark late")
    @PatchMapping("/{attendanceId}/late")
    public ResponseEntity<AttendanceResponse> markLate(
            @PathVariable UUID attendanceId
    ) {

        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.markLate(
                                attendanceId
                        )
                )
        );
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_MARK_REMOTE')")
    @Operation(summary = "Mark remote")
    @PatchMapping("/{attendanceId}/remote")
    public ResponseEntity<AttendanceResponse> markRemote(
            @PathVariable UUID attendanceId
    ) {

        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.markRemote(
                                attendanceId
                        )
                )
        );
    }

    @PreAuthorize(
            "hasAuthority('ATTENDANCE_READ') and @employeeAuthorizationService.canManage(authentication)"
    )
    @Operation(summary = "Get by date")
    @GetMapping("/date/{date}")
    public ResponseEntity<Page<AttendanceResponse>> getByDate(
            @PathVariable LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                attendanceService
                        .getByDate(date, pageable)
                        .map(AttendanceResponse::from)
        );
    }

    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "attendanceDate"
                )
        );
    }
}