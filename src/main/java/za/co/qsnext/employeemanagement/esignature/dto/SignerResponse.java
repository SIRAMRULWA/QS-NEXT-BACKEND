package za.co.qsnext.employeemanagement.esignature.dto;

import za.co.qsnext.employeemanagement.esignature.SignatureRequestSigner;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SignerResponse(
        UUID id,
        UUID signerUserId,
        String status,
        OffsetDateTime signedAt,
        String declineReason
) {

    public static SignerResponse from(SignatureRequestSigner signer) {
        return new SignerResponse(
                signer.getId(),
                signer.getSignerUserId(),
                signer.getStatus(),
                signer.getSignedAt(),
                signer.getDeclineReason()
        );
    }
}
