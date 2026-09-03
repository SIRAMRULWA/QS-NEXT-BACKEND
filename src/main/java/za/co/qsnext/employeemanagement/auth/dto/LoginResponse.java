package za.co.qsnext.employeemanagement.auth.dto;

import za.co.qsnext.employeemanagement.user.User;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String email
) {

    public static LoginResponse fromAuthenticatedUser(User user) {

        return new LoginResponse(
                null,
                null,
                "Bearer",
                0,
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}