package za.co.qsnext.employeemanagement.team.dto;

import za.co.qsnext.employeemanagement.employee.Employee;

import java.util.UUID;

/**
 * Just enough about a direct report for a manager to recognize them and
 * act on their requests; no phone number, hire date or employee number.
 */
public record TeamMemberResponse(
        UUID id,
        String firstName,
        String lastName,
        String jobTitle,
        String employmentStatus
) {

    public static TeamMemberResponse from(Employee employee) {
        return new TeamMemberResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getJobTitle(),
                employee.getEmploymentStatus()
        );
    }
}
