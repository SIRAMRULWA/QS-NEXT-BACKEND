package za.co.qsnext.employeemanagement.employee.dto;

import za.co.qsnext.employeemanagement.employee.Employee;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID userId,
        UUID departmentId,
        String employeeNumber,
        String firstName,
        String lastName,
        String phoneNumber,
        String jobTitle,
        LocalDate hireDate,
        String employmentStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUserId(),
                employee.getDepartmentId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhoneNumber(),
                employee.getJobTitle(),
                employee.getHireDate(),
                employee.getEmploymentStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt(),
                employee.getVersion()
        );
    }
}