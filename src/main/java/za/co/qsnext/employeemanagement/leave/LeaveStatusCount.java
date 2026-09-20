package za.co.qsnext.employeemanagement.leave;

/**
 * Spring Data interface projection for the leave-by-status aggregate
 * query used by the Analytics module.
 */
public interface LeaveStatusCount {

    String getStatus();

    Long getRequestCount();
}
