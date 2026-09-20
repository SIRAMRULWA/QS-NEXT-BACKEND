package za.co.qsnext.employeemanagement.directory.dto;

import za.co.qsnext.employeemanagement.department.Department;

import java.util.UUID;

public record DirectoryDepartmentResponse(
        UUID id,
        String name,
        String description
) {

    public static DirectoryDepartmentResponse from(Department department) {
        return new DirectoryDepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription()
        );
    }
}
