package za.co.qsnext.employeemanagement.timesheet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final EmployeeService employeeService;
    private final UserService userService;

    public TimesheetService(
            TimesheetRepository timesheetRepository,
            TimesheetEntryRepository timesheetEntryRepository,
            EmployeeService employeeService,
            UserService userService
    ) {
        this.timesheetRepository = timesheetRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.employeeService = employeeService;
        this.userService = userService;
    }

    public Timesheet getById(UUID timesheetId) {

        return timesheetRepository.findById(timesheetId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Timesheet not found: " + timesheetId
                        )
                );
    }

    public Page<Timesheet> getByEmployee(
            UUID employeeId,
            Pageable pageable
    ) {

        employeeService.getById(employeeId);

        return timesheetRepository.findByEmployeeId(
                employeeId,
                pageable
        );
    }

    public Page<Timesheet> getByStatus(
            String status,
            Pageable pageable
    ) {

        return timesheetRepository.findByStatus(
                status,
                pageable
        );
    }

    public List<TimesheetEntry> getEntries(
            UUID timesheetId
    ) {

        getById(timesheetId);

        return timesheetEntryRepository
                .findByTimesheetIdOrderByWorkDateAsc(
                        timesheetId
                );
    }

    @Transactional
    public Timesheet create(
            UUID employeeId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {

        employeeService.getById(employeeId);

        validatePeriod(periodStart, periodEnd);

        if (timesheetRepository
                .existsByEmployeeIdAndPeriodStartAndPeriodEnd(
                        employeeId,
                        periodStart,
                        periodEnd
                )) {

            throw new BusinessRuleException(
                    "Timesheet already exists for this period"
            );
        }

        return timesheetRepository.save(
                new Timesheet(
                        employeeId,
                        periodStart,
                        periodEnd
                )
        );
    }

    @Transactional
    public TimesheetEntry addEntry(
            UUID timesheetId,
            LocalDate workDate,
            BigDecimal hoursWorked,
            String description
    ) {

        Timesheet timesheet = getById(timesheetId);

        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessRuleException(
                    "Entries can only be added to a draft timesheet"
            );
        }

        if (workDate.isBefore(timesheet.getPeriodStart())
                || workDate.isAfter(timesheet.getPeriodEnd())) {

            throw new BusinessRuleException(
                    "Work date must fall within the timesheet period"
            );
        }

        if (hoursWorked == null
                || hoursWorked.compareTo(BigDecimal.ZERO) < 0
                || hoursWorked.compareTo(BigDecimal.valueOf(24)) > 0) {

            throw new BusinessRuleException(
                    "Hours worked must be between 0 and 24"
            );
        }

        if (timesheetEntryRepository
                .findByTimesheetIdAndWorkDate(
                        timesheetId,
                        workDate
                )
                .isPresent()) {

            throw new BusinessRuleException(
                    "A timesheet entry already exists for this date"
            );
        }

        return timesheetEntryRepository.save(
                new TimesheetEntry(
                        timesheetId,
                        workDate,
                        hoursWorked,
                        description
                )
        );
    }

    @Transactional
    public TimesheetEntry updateEntry(
            UUID entryId,
            BigDecimal hoursWorked,
            String description
    ) {

        TimesheetEntry entry =
                timesheetEntryRepository.findById(entryId)
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Timesheet entry not found: " + entryId
                                )
                        );

        Timesheet timesheet = getById(entry.getTimesheetId());

        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessRuleException(
                    "Only draft timesheets can be modified"
            );
        }

        if (hoursWorked == null
                || hoursWorked.compareTo(BigDecimal.ZERO) < 0
                || hoursWorked.compareTo(BigDecimal.valueOf(24)) > 0) {

            throw new BusinessRuleException(
                    "Hours worked must be between 0 and 24"
            );
        }

        entry.update(hoursWorked, description);

        return entry;
    }

    @Transactional
    public void deleteEntry(UUID entryId) {

        TimesheetEntry entry =
                timesheetEntryRepository.findById(entryId)
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Timesheet entry not found: " + entryId
                                )
                        );

        Timesheet timesheet = getById(entry.getTimesheetId());

        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessRuleException(
                    "Only draft timesheets can be modified"
            );
        }

        timesheetEntryRepository.delete(entry);
    }

    @Transactional
    public Timesheet submit(UUID timesheetId) {

        Timesheet timesheet = getById(timesheetId);

        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessRuleException(
                    "Only draft timesheets can be submitted"
            );
        }

        List<TimesheetEntry> entries =
                timesheetEntryRepository
                        .findByTimesheetIdOrderByWorkDateAsc(
                                timesheetId
                        );

        if (entries.isEmpty()) {
            throw new BusinessRuleException(
                    "A timesheet must contain at least one entry before submission"
            );
        }

        timesheet.submit();

        return timesheet;
    }

    @Transactional
    public Timesheet approve(
            UUID timesheetId,
            UUID approverId
    ) {

        Timesheet timesheet = getById(timesheetId);

        userService.getById(approverId);

        if (!"SUBMITTED".equals(timesheet.getStatus())) {
            throw new BusinessRuleException(
                    "Only submitted timesheets can be approved"
            );
        }

        timesheet.approve(approverId);

        return timesheet;
    }

    @Transactional
    public Timesheet reject(UUID timesheetId) {

        Timesheet timesheet = getById(timesheetId);

        if (!"SUBMITTED".equals(timesheet.getStatus())) {
            throw new BusinessRuleException(
                    "Only submitted timesheets can be rejected"
            );
        }

        timesheet.reject();

        return timesheet;
    }

    private void validatePeriod(
            LocalDate periodStart,
            LocalDate periodEnd
    ) {

        if (periodStart == null || periodEnd == null) {
            throw new BusinessRuleException(
                    "Timesheet period start and end dates are required"
            );
        }

        if (periodEnd.isBefore(periodStart)) {
            throw new BusinessRuleException(
                    "Timesheet period end cannot be before period start"
            );
        }
    }
}