package za.co.qsnext.employeemanagement.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;

    @Column(
            name = "description",
            length = 255
    )
    private String description;

    protected Role() {
        // Required by JPA
    }

    public Role(
            UUID id,
            String name,
            String description
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}