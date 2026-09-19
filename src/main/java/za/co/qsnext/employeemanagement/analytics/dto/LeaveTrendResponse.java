package za.co.qsnext.employeemanagement.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record LeaveTrendResponse(LocalDate from, LocalDate to, List<StatusCount> byStatus) {
}
