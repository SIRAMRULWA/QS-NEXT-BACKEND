package za.co.qsnext.employeemanagement.selfservice.dto;

import za.co.qsnext.employeemanagement.employee.Employee;

import java.time.LocalDate;
import java.util.UUID;

public record SelfServiceProfileResponse(
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

    public static SelfServiceProfileResponse from(
            Employee employee
    ) {
        return new SelfServiceProfileResponse(
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