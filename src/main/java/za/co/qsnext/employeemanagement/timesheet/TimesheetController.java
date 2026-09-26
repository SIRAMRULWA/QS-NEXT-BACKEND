package za.co.qsnext.employeemanagement.timesheet;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.timesheet.dto.*;

import java.util.UUID;

@Tag(name = "Timesheets", description = "Timesheet periods, entries, submission and approval.")
@RestController
@RequestMapping("/api/v1/timesheets")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(
            TimesheetService timesheetService
    ) {
        this.timesheetService = timesheetService;
    }

    @PreAuthorize(
            "hasAuthority('TIMESHEET_READ') and " +
                    "@timesheetAuthorizationService.canReadTimesheet(#timesheetId, authentication)"
    )
    @Operation(summary = "Get by id")
    @GetMapping("/{timesheetId}")
    public ResponseEntity<TimesheetResponse> getById(
            @PathVariable UUID timesheetId
    ) {

        return ResponseEntity.ok(
                TimesheetResponse.from(
                        timesheetService.getById(timesheetId)
                )
        );
    }

    @PreAuthorize(
            "hasAuthority('TIMESHEET_CREATE') and " +
                    "@employeeAuthorizationService.canActFor(#request.employeeId, authentication)"
    )
    @Operation(summary = "Create")
    @PostMapping
    public ResponseEntity<TimesheetResponse> create(
            @Valid @RequestBody CreateTimesheetRequest request
    ) {

        Timesheet timesheet =
                timesheetService.create(
                        request.employeeId(),
                        request.periodStart(),
                        request.periodEnd()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        TimesheetResponse.from(timesheet)
                );
    }

    @PreAuthorize(
            "hasAuthority('TIMESHEET_ENTRY_CREATE') and " +
                    "@timesheetAuthorizationService.canActOnTimesheet(#timesheetId, authentication)"
    )
    @Operation(summary = "Add entry")
    @PostMapping("/{timesheetId}/entries")
    public ResponseEntity<Void> addEntry(
            @PathVariable UUID timesheetId,
            @Valid @RequestBody CreateTimesheetEntryRequest request
    ) {

        timesheetService.addEntry(
                timesheetId,
                request.workDate(),
                request.hoursWorked(),
                request.description()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PreAuthorize(
            "hasAuthority('TIMESHEET_SUBMIT') and " +
                    "@timesheetAuthorizationService.canActOnTimesheet(#timesheetId, authentication)"
    )
    @Operation(summary = "Submit")
    @PostMapping("/{timesheetId}/submit")
    public ResponseEntity<TimesheetResponse> submit(
            @PathVariable UUID timesheetId
    ) {

        return ResponseEntity.ok(
                TimesheetResponse.from(
                        timesheetService.submit(timesheetId)
                )
        );
    }

    @PreAuthorize(
            "hasAuthority('TIMESHEET_APPROVE') and " +
                    "@timesheetAuthorizationService.canApproveTimesheet(#timesheetId, authentication)"
    )
    @Operation(summary = "Approve")
    @PostMapping("/{timesheetId}/approve")
    public ResponseEntity<TimesheetResponse> approve(
            @PathVariable UUID timesheetId,
            Authentication authentication
    ) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                TimesheetResponse.from(
                        timesheetService.approve(
                                timesheetId,
                                userDetails.getUserId()
                        )
                )
        );
    }

    @PreAuthorize(
            "hasAuthority('TIMESHEET_REJECT') and " +
                    "@timesheetAuthorizationService.canApproveTimesheet(#timesheetId, authentication)"
    )
    @Operation(summary = "Reject")
    @PostMapping("/{timesheetId}/reject")
    public ResponseEntity<TimesheetResponse> reject(
            @PathVariable UUID timesheetId
    ) {

        return ResponseEntity.ok(
                TimesheetResponse.from(
                        timesheetService.reject(timesheetId)
                )
        );
    }

    @PreAuthorize("hasAuthority('TIMESHEET_ENTRY_DELETE')")
    @Operation(summary = "Delete entry")
    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID entryId
    ) {

        timesheetService.deleteEntry(entryId);

        return ResponseEntity.noContent().build();
    }
}