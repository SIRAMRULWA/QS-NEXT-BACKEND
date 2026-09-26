package za.co.qsnext.employeemanagement.team;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.expense.ExpenseClaim;
import za.co.qsnext.employeemanagement.expense.ExpenseClaimRepository;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseClaimResponse;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import za.co.qsnext.employeemanagement.security.EmployeeAuthorizationService;
import za.co.qsnext.employeemanagement.team.dto.TeamMemberResponse;
import za.co.qsnext.employeemanagement.team.dto.TeamOverviewResponse;
import za.co.qsnext.employeemanagement.timesheet.TimesheetRepository;
import za.co.qsnext.employeemanagement.timesheet.dto.TimesheetResponse;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private static final String LEAVE_PENDING = "PENDING";
    private static final String TIMESHEET_SUBMITTED = "SUBMITTED";

    private final EmployeeAuthorizationService employeeAuthorizationService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TimesheetRepository timesheetRepository;
    private final ExpenseClaimRepository expenseClaimRepository;

    public TeamService(
            EmployeeAuthorizationService employeeAuthorizationService,
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository,
            TimesheetRepository timesheetRepository,
            ExpenseClaimRepository expenseClaimRepository
    ) {
        this.employeeAuthorizationService = employeeAuthorizationService;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.timesheetRepository = timesheetRepository;
        this.expenseClaimRepository = expenseClaimRepository;
    }

    @Transactional(readOnly = true)
    public TeamOverviewResponse getOverview(Authentication authentication) {

        List<UUID> reportIds = employeeAuthorizationService.directReportIds(authentication);

        if (reportIds.isEmpty()) {
            return new TeamOverviewResponse(List.of(), List.of(), List.of(), List.of());
        }

        return new TeamOverviewResponse(
                employeeRepository.findAllById(reportIds).stream()
                        .map(TeamMemberResponse::from)
                        .toList(),
                leaveRequestRepository
                        .findByEmployeeIdInAndStatusOrderByStartDateAsc(reportIds, LEAVE_PENDING).stream()
                        .map(LeaveResponse::from)
                        .toList(),
                timesheetRepository
                        .findByEmployeeIdInAndStatusOrderByPeriodStartAsc(reportIds, TIMESHEET_SUBMITTED).stream()
                        .map(TimesheetResponse::from)
                        .toList(),
                expenseClaimRepository
                        .findByEmployeeIdInAndStatusOrderByExpenseDateAsc(reportIds, ExpenseClaim.STATUS_SUBMITTED)
                        .stream()
                        .map(ExpenseClaimResponse::from)
                        .toList()
        );
    }
}
