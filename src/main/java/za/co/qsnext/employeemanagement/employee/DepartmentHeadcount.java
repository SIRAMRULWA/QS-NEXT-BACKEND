package za.co.qsnext.employeemanagement.employee;

import java.util.UUID;

/**
 * Spring Data interface projection for the headcount-by-department
 * aggregate query used by the Analytics module.
 */
public interface DepartmentHeadcount {

    UUID getDepartmentId();

    Long getEmployeeCount();
}
