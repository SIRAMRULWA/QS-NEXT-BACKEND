package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.employee.Employee;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeReportResponse(
        UUID employeeId,
        UUID userId,
        UUID departmentId,
        String employeeNumber,
        String firstName,
        String lastName,
        String phoneNumber,
        String jobTitle,
        LocalDate hireDate,
        String employmentStatus
) {

    public static EmployeeReportResponse from(Employee employee) {
        return new EmployeeReportResponse(
                employee.getId(),
                employee.getUserId(),
                employee.getDepartmentId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhoneNumber(),
                employee.getJobTitle(),
                employee.getHireDate(),
                employee.getEmploymentStatus()
        );
    }
}