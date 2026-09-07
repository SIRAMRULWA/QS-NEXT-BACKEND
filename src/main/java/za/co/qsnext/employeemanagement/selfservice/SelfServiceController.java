package za.co.qsnext.employeemanagement.selfservice;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;

import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;

@RestController
@RequestMapping("/api/v1/self-service")
public class SelfServiceController {

    private final SelfServiceService selfServiceService;

    public SelfServiceController(
            SelfServiceService selfServiceService
    ) {
        this.selfServiceService = selfServiceService;
    }

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

    @GetMapping("/leave")
    @PreAuthorize("hasAuthority('LEAVE_READ')")
    public ResponseEntity<Page<LeaveResponse>> getOwnLeave(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        int safeSize = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
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

    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    @GetMapping("/attendance")
    public ResponseEntity<Page<AttendanceResponse>> getOwnAttendance(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
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
}