package za.co.qsnext.employeemanagement.attendance;

/**
 * Spring Data interface projection for the attendance-by-status
 * aggregate query used by the Analytics module.
 */
public interface AttendanceStatusCount {

    String getStatus();

    Long getRecordCount();
}
