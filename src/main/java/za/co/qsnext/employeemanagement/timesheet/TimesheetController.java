package za.co.qsnext.employeemanagement.timesheet;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.qsnext.employeemanagement.timesheet.dto.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timesheets")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(
            TimesheetService timesheetService
    ) {
        this.timesheetService = timesheetService;
    }

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

    @PostMapping("/{timesheetId}/approve")
    public ResponseEntity<TimesheetResponse> approve(
            @PathVariable UUID timesheetId,
            @Valid @RequestBody TimesheetApprovalRequest request
    ) {

        return ResponseEntity.ok(
                TimesheetResponse.from(
                        timesheetService.approve(
                                timesheetId,
                                request.approverId()
                        )
                )
        );
    }

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

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID entryId
    ) {

        timesheetService.deleteEntry(entryId);

        return ResponseEntity.noContent().build();
    }
}