package za.co.qsnext.employeemanagement.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "learning_path_courses")
public class LearningPathCourse {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "learning_path_id", nullable = false, updatable = false)
    private UUID learningPathId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected LearningPathCourse() {
        // Required by JPA
    }

    public LearningPathCourse(UUID learningPathId, UUID courseId, int sortOrder) {
        this.learningPathId = learningPathId;
        this.courseId = courseId;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLearningPathId() {
        return learningPathId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
