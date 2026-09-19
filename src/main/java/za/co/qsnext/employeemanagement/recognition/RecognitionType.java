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
 * A configurable recognition award type (e.g. "Above and Beyond", "Team
 * Player"), each worth a number of points - the "points/rewards
 * abstraction" the recognition module is built on.
 */
@Entity
@Table(name = "recognition_types")
public class RecognitionType {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "point_value", nullable = false)
    private int pointValue;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RecognitionType() {
        // Required by JPA
    }

    public RecognitionType(String name, String description, int pointValue) {
        this.name = name;
        this.description = description;
        this.pointValue = pointValue;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPointValue() {
        return pointValue;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
