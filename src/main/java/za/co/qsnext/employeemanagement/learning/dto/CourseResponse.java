package za.co.qsnext.employeemanagement.learning.dto;

import za.co.qsnext.employeemanagement.learning.Course;

import java.util.UUID;

public record CourseResponse(
        UUID id,
        String title,
        String description,
        String category,
        Integer durationMinutes,
        boolean mandatory,
        UUID linkedComplianceRequirementId,
        boolean active
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory(),
                course.getDurationMinutes(),
                course.isMandatory(),
                course.getLinkedComplianceRequirementId(),
                course.isActive()
        );
    }
}
