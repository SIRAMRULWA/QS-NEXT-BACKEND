package za.co.qsnext.employeemanagement.analytics.dto;

import java.util.List;

public record HeadcountAnalyticsResponse(long totalEmployees, List<StatusCount> byEmploymentStatus) {
}
