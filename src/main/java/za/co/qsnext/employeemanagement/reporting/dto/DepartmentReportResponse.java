package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.employee.Employee;

import java.util.List;
import java.util.UUID;

public record DepartmentReportResponse(
        UUID departmentId,
        String name,
        String description,
        int employeeCount,
        List<EmployeeSummary> employees
) {

    public static DepartmentReportResponse from(Department department, List<Employee> employees) {
        return new DepartmentReportResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                employees.size(),
                employees.stream().map(EmployeeSummary::from).toList()
        );
    }

    public record EmployeeSummary(
            UUID employeeId,
            String employeeNumber,
            String firstName,
            String lastName,
            String jobTitle,
            String employmentStatus
    ) {

        public static EmployeeSummary from(Employee employee) {
            return new EmployeeSummary(
                    employee.getId(),
                    employee.getEmployeeNumber(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getJobTitle(),
                    employee.getEmploymentStatus()
            );
        }
    }
}
