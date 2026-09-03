package za.co.qsnext.employeemanagement.employee.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EmployeeSearchRequest(

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        UUID departmentId,

        @Size(max = 30, message = "Employment status must not exceed 30 characters")
        String employmentStatus
) {
}