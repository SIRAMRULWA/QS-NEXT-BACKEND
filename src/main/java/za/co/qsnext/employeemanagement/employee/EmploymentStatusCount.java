package za.co.qsnext.employeemanagement.employee;

/**
 * Spring Data interface projection for the headcount-by-status
 * aggregate query used by the Analytics module.
 */
public interface EmploymentStatusCount {

    String getStatus();

    Long getEmployeeCount();
}
