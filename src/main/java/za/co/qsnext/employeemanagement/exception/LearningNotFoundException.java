package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing Skill, Course, LearningPath or CourseEnrollment.
 */
public class LearningNotFoundException extends RuntimeException {

    public LearningNotFoundException(String message) {
        super(message);
    }
}
