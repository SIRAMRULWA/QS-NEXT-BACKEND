package za.co.qsnext.employeemanagement.analytics.dto;

import java.util.List;

public record RecruitmentFunnelResponse(List<StatusCount> byStage) {
}
