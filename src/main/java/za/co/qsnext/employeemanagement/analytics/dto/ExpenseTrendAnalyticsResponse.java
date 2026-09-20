package za.co.qsnext.employeemanagement.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseTrendAnalyticsResponse(LocalDate from, LocalDate to, BigDecimal totalAmount, long claimCount) {
}
