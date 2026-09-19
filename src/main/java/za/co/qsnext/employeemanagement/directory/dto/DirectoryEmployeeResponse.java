package za.co.qsnext.employeemanagement.directory.dto;

import za.co.qsnext.employeemanagement.employee.Employee;

import java.util.UUID;

public record DirectoryEmployeeResponse(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String jobTitle,
        String phoneNumber,
        String employmentStatus,
        UUID departmentId,
        String departmentName,
        UUID managerId,
        String managerName
) {

    public static DirectoryEmployeeResponse from(
            Employee employee,
            String departmentName,
            String managerName
    ) {
        return new DirectoryEmployeeResponse(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getJobTitle(),
                employee.getPhoneNumber(),
                employee.getEmploymentStatus(),
                employee.getDepartmentId(),
                departmentName,
                employee.getManagerId(),
                managerName
        );
    }
}
