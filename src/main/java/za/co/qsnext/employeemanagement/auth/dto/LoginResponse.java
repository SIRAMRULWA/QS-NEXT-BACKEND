package za.co.qsnext.employeemanagement.auth.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record LoginResponse(

        String accessToken,

        String tokenType,

        long expiresIn,

        UUID userId,

        String username,

        Set<String> roles,

        OffsetDateTime issuedAt
) {
}