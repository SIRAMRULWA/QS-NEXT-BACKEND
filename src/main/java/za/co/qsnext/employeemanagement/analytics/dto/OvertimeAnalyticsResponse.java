package za.co.qsnext.employeemanagement.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OvertimeAnalyticsResponse(LocalDate from, LocalDate to, BigDecimal totalOvertimeAmount) {
}
