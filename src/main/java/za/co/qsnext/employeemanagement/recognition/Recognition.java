package za.co.qsnext.employeemanagement.recognition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One act of recognition, given by a user to an employee. Whether it
 * counts as "peer" or "manager" recognition is a function of the
 * giver's role at the time, not a stored field - keeping this a single,
 * simple entity rather than two parallel ones.
 * <p>
 * {@link #points} is a snapshot of the {@link RecognitionType}'s point
 * value at the time this was given, so a later change to the type's
 * value doesn't retroactively rewrite history.
 */
@Entity
@Table(name = "recognitions")
public class Recognition {

    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "type_id", nullable = false, updatable = false)
    private UUID typeId;

    @Column(name = "given_by_user_id", nullable = false, updatable = false)
    private UUID givenByUserId;

    @Column(name = "given_to_employee_id", nullable = false, updatable = false)
    private UUID givenToEmployeeId;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "points", nullable = false, updatable = false)
    private int points;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Recognition() {
        // Required by JPA
    }

    public Recognition(
            UUID typeId,
            UUID givenByUserId,
            UUID givenToEmployeeId,
            String message,
            int points,
            String visibility
    ) {
        this.typeId = typeId;
        this.givenByUserId = givenByUserId;
        this.givenToEmployeeId = givenToEmployeeId;
        this.message = message;
        this.points = points;
        this.visibility = visibility;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public UUID getGivenByUserId() {
        return givenByUserId;
    }

    public UUID getGivenToEmployeeId() {
        return givenToEmployeeId;
    }

    public String getMessage() {
        return message;
    }

    public int getPoints() {
        return points;
    }

    public String getVisibility() {
        return visibility;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
