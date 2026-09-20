package za.co.qsnext.employeemanagement.esignature.dto;

import za.co.qsnext.employeemanagement.esignature.SignatureRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SignatureRequestResponse(
        UUID id,
        UUID documentId,
        String title,
        UUID requestedBy,
        String status,
        OffsetDateTime expiresAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        List<SignerResponse> signers
) {

    public static SignatureRequestResponse from(SignatureRequest request, List<SignerResponse> signers) {
        return new SignatureRequestResponse(
                request.getId(),
                request.getDocumentId(),
                request.getTitle(),
                request.getRequestedBy(),
                request.getStatus(),
                request.getExpiresAt(),
                request.getCompletedAt(),
                request.getCreatedAt(),
                signers
        );
    }
}
