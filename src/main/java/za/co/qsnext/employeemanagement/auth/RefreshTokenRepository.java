package za.co.qsnext.employeemanagement.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    @Modifying
    @Query("""
            update RefreshToken r
            set r.revokedAt = :revokedAt
            where r.userId = :userId
            and r.revokedAt is null
            """)
    int revokeAllActiveForUser(
            @Param("userId") UUID userId,
            @Param("revokedAt") OffsetDateTime revokedAt
    );
}
