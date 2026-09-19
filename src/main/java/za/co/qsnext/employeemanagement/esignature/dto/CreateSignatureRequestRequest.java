package za.co.qsnext.employeemanagement.esignature.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateSignatureRequestRequest(

        @NotNull(message = "Document ID is required")
        UUID documentId,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotEmpty(message = "At least one signer is required")
        List<UUID> signerUserIds,

        @NotNull(message = "Expiry is required")
        @Future(message = "Expiry must be in the future")
        OffsetDateTime expiresAt
) {
}
