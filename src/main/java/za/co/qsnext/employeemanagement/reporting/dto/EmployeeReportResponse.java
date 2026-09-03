package za.co.qsnext.employeemanagement.reporting.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeReportResponse(

        UUID employeeId,

        String employeeNumber,

        String firstName,

        String lastName,

        String departmentName,

        String jobTitle,

        String employmentStatus,

        LocalDate hireDate
) {
}