package za.co.qsnext.employeemanagement.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    public static final String TYPE_MEETING = "MEETING";
    public static final String TYPE_COMPANY = "COMPANY";
    public static final String TYPE_HOLIDAY = "HOLIDAY";
    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_SHIFT = "SHIFT";
    public static final String TYPE_INTERVIEW = "INTERVIEW";

    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_DEPARTMENT = "DEPARTMENT";
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CalendarEvent() {
        // Required by JPA
    }

    public CalendarEvent(
            String title,
            String description,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            boolean allDay,
            String eventType,
            String visibility,
            UUID ownerUserId,
            UUID departmentId
    ) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.eventType = eventType;
        this.visibility = visibility;
        this.ownerUserId = ownerUserId;
        this.departmentId = departmentId;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public String getEventType() {
        return eventType;
    }

    public String getVisibility() {
        return visibility;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
