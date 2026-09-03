package za.co.qsnext.employeemanagement.leave;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.leave.dto.CreateLeaveRequest;
import za.co.qsnext.employeemanagement.leave.dto.LeaveApprovalRequest;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PreAuthorize("hasAuthority('LEAVE_READ')")
    @GetMapping("/{leaveRequestId}")
    public ResponseEntity<LeaveResponse> getById(
            @PathVariable UUID leaveRequestId
    ) {

        return ResponseEntity.ok(
                LeaveResponse.from(
                        leaveService.getById(leaveRequestId)
                )
        );
    }

    @PreAuthorize("hasAuthority('LEAVE_READ')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<LeaveResponse>> getByEmployee(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                leaveService
                        .getByEmployee(employeeId, pageable)
                        .map(LeaveResponse::from)
        );
    }

    @PreAuthorize("hasAuthority('LEAVE_READ')")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<LeaveResponse>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                leaveService
                        .getByStatus(status, pageable)
                        .map(LeaveResponse::from)
        );
    }

    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    @PostMapping
    public ResponseEntity<LeaveResponse> create(
            @Valid @RequestBody CreateLeaveRequest request
    ) {

        LeaveRequest leaveRequest =
                leaveService.create(
                        request.employeeId(),
                        request.leaveType(),
                        request.startDate(),
                        request.endDate(),
                        request.reason()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        LeaveResponse.from(leaveRequest)
                );
    }

    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    @PostMapping("/{leaveRequestId}/approve")
    public ResponseEntity<LeaveResponse> approve(
            @PathVariable UUID leaveRequestId,
            @Valid @RequestBody LeaveApprovalRequest request
    ) {

        LeaveRequest leaveRequest =
                leaveService.approve(
                        leaveRequestId,
                        request.approverId()
                );

        return ResponseEntity.ok(
                LeaveResponse.from(leaveRequest)
        );
    }

    @PreAuthorize("hasAuthority('LEAVE_REJECT')")
    @PostMapping("/{leaveRequestId}/reject")
    public ResponseEntity<LeaveResponse> reject(
            @PathVariable UUID leaveRequestId
    ) {

        return ResponseEntity.ok(
                LeaveResponse.from(
                        leaveService.reject(leaveRequestId)
                )
        );
    }

    @PreAuthorize("hasAuthority('LEAVE_CANCEL')")
    @PostMapping("/{leaveRequestId}/cancel")
    public ResponseEntity<LeaveResponse> cancel(
            @PathVariable UUID leaveRequestId
    ) {

        return ResponseEntity.ok(
                LeaveResponse.from(
                        leaveService.cancel(leaveRequestId)
                )
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
                        "createdAt"
                )
        );
    }
}