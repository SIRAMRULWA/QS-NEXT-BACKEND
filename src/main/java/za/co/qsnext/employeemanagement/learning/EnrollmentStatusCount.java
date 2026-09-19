package za.co.qsnext.employeemanagement.learning;

/**
 * Spring Data interface projection for the learning-activity
 * aggregate query used by the Analytics module.
 */
public interface EnrollmentStatusCount {

    String getStatus();

    Long getEnrollmentCount();
}
