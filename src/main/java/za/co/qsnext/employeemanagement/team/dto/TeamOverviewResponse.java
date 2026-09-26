package za.co.qsnext.employeemanagement.team.dto;

import za.co.qsnext.employeemanagement.expense.dto.ExpenseClaimResponse;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import za.co.qsnext.employeemanagement.timesheet.dto.TimesheetResponse;

import java.util.List;

public record TeamOverviewResponse(
        List<TeamMemberResponse> members,
        List<LeaveResponse> pendingLeave,
        List<TimesheetResponse> pendingTimesheets,
        List<ExpenseClaimResponse> pendingExpenseClaims
) {
}
