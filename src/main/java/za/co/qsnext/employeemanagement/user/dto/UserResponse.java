package za.co.qsnext.employeemanagement.user.dto;

import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        boolean locked,
        Set<String> roles,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.isLocked(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
