package za.co.qsnext.employeemanagement.notification;

/**
 * Known in-app notification types. Kept as an enum (rather than a free
 * string at every call site) so callers can't typo a type, while the
 * persisted column stays a plain VARCHAR of {@code name()} for forward
 * compatibility with types added later.
 */
public enum NotificationType {

    LEAVE_REQUEST_APPROVED,
    LEAVE_REQUEST_REJECTED,
    TIMESHEET_APPROVED,
    TIMESHEET_REJECTED,
    ONBOARDING_STARTED,
    ONBOARDING_COMPLETED,
    SIGNATURE_REQUESTED,
    SIGNATURE_REQUEST_COMPLETED,
    SIGNATURE_REQUEST_DECLINED,
    COMPLIANCE_TASK_ASSIGNED,
    COURSE_COMPLETED,
    PERFORMANCE_REVIEW_ASSIGNED,
    PERFORMANCE_REVIEW_COMPLETED,
    RECOGNITION_RECEIVED
}
