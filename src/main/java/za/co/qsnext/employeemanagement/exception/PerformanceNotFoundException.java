package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing PerformanceCycle, PerformanceGoal, PerformanceReview
 * or DevelopmentPlan.
 */
public class PerformanceNotFoundException extends RuntimeException {

    public PerformanceNotFoundException(String message) {
        super(message);
    }
}
