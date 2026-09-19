package za.co.qsnext.employeemanagement.learning.dto;

import za.co.qsnext.employeemanagement.learning.LearningPath;

import java.util.List;
import java.util.UUID;

public record LearningPathResponse(
        UUID id,
        String name,
        String description,
        List<CourseResponse> courses
) {

    public static LearningPathResponse from(LearningPath path, List<CourseResponse> courses) {
        return new LearningPathResponse(path.getId(), path.getName(), path.getDescription(), courses);
    }
}
