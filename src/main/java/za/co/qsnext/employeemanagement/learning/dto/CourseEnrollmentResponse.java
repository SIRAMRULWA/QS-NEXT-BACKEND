package za.co.qsnext.employeemanagement.learning.dto;

import za.co.qsnext.employeemanagement.learning.CourseEnrollment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CourseEnrollmentResponse(
        UUID id,
        UUID employeeId,
        UUID courseId,
        String status,
        int progressPercent,
        OffsetDateTime enrolledAt,
        OffsetDateTime completedAt,
        OffsetDateTime certificateIssuedAt
) {

    public static CourseEnrollmentResponse from(CourseEnrollment enrollment) {
        return new CourseEnrollmentResponse(
                enrollment.getId(),
                enrollment.getEmployeeId(),
                enrollment.getCourseId(),
                enrollment.getStatus(),
                enrollment.getProgressPercent(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt(),
                enrollment.getCertificateIssuedAt()
        );
    }
}
