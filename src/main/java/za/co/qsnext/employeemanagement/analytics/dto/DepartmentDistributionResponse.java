package za.co.qsnext.employeemanagement.analytics.dto;

import java.util.List;
import java.util.UUID;

public record DepartmentDistributionResponse(List<DepartmentCount> departments) {

    public record DepartmentCount(UUID departmentId, String departmentName, long employeeCount) {
    }
}
