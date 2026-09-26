package za.co.qsnext.employeemanagement.recruitment;

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

/**
 * A person who has applied (or could apply) for a role. A candidate HR
 * enters by hand has no User account; one who applied through the job
 * board is linked to their APPLICANT login via {@link #getUserId()}.
 * Either way, communication goes out by plain email (see
 * {@code EmailTemplate#INTERVIEW_INVITATION}/{@code #OFFER_EXTENDED}).
 */
@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "resume_document_id")
    private UUID resumeDocumentId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Candidate() {
        // Required by JPA
    }

    public Candidate(
            String firstName,
            String lastName,
            String email,
            String phone,
            UUID resumeDocumentId,
            String source
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.resumeDocumentId = resumeDocumentId;
        this.source = source;
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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UUID getResumeDocumentId() {
        return resumeDocumentId;
    }

    public String getSource() {
        return source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public UUID getUserId() {
        return userId;
    }

    /**
     * Links this candidate to the self-signup login that applied, so the
     * applicant can track their applications and a hire reuses the login.
     */
    public void linkUser(UUID userId) {
        this.userId = userId;
    }
}
