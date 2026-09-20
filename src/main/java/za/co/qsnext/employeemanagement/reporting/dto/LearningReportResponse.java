package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.learning.CourseEnrollment;
import za.co.qsnext.employeemanagement.learning.EmployeeSkill;

import java.util.List;
import java.util.UUID;

public record LearningReportResponse(
        UUID employeeId,
        int totalEnrollments,
        int completedEnrollments,
        int inProgressEnrollments,
        int enrolledEnrollments,
        List<EnrollmentSummary> enrollments,
        List<SkillSummary> skills
) {

    public static LearningReportResponse from(
            UUID employeeId, List<CourseEnrollment> enrollments, List<EmployeeSkill> skills) {

        int completed = 0;
        int inProgress = 0;
        int enrolled = 0;

        for (CourseEnrollment enrollment : enrollments) {
            switch (enrollment.getStatus()) {
                case CourseEnrollment.STATUS_COMPLETED -> completed++;
                case CourseEnrollment.STATUS_IN_PROGRESS -> inProgress++;
                case CourseEnrollment.STATUS_ENROLLED -> enrolled++;
                default -> {
                    // Database CHECK constraint prevents unknown statuses.
                }
            }
        }

        return new LearningReportResponse(
                employeeId, enrollments.size(), completed, inProgress, enrolled,
                enrollments.stream().map(EnrollmentSummary::from).toList(),
                skills.stream().map(SkillSummary::from).toList());
    }

    public record EnrollmentSummary(
            UUID id,
            UUID courseId,
            String status,
            int progressPercent
    ) {

        public static EnrollmentSummary from(CourseEnrollment enrollment) {
            return new EnrollmentSummary(
                    enrollment.getId(), enrollment.getCourseId(),
                    enrollment.getStatus(), enrollment.getProgressPercent());
        }
    }

    public record SkillSummary(
            UUID id,
            UUID skillId,
            String proficiencyLevel
    ) {

        public static SkillSummary from(EmployeeSkill skill) {
            return new SkillSummary(skill.getId(), skill.getSkillId(), skill.getProficiencyLevel());
        }
    }
}
