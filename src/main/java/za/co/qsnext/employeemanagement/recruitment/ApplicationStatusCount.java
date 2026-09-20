package za.co.qsnext.employeemanagement.recruitment;

/**
 * Spring Data interface projection for the recruitment-funnel
 * aggregate query used by the Analytics module.
 */
public interface ApplicationStatusCount {

    String getStatus();

    Long getApplicationCount();
}
