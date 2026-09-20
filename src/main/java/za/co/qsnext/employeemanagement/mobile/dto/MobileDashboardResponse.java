package za.co.qsnext.employeemanagement.mobile.dto;

import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.calendar.dto.CalendarEventResponse;
import za.co.qsnext.employeemanagement.leave.dto.LeaveBalanceResponse;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;

import java.util.List;

/**
 * A single, mobile-friendly composite read of everything a mobile
 * client's home screen typically needs, built purely by composing
 * existing domain services (see {@code MobileService}) - saving a
 * mobile client the round trips of calling profile, attendance, leave
 * balance, calendar and notification endpoints separately.
 */
public record MobileDashboardResponse(
        SelfServiceProfileResponse profile,
        AttendanceResponse todayAttendance,
        List<LeaveBalanceResponse> leaveBalances,
        List<CalendarEventResponse> upcomingEvents,
        long unreadNotificationCount
) {
}
