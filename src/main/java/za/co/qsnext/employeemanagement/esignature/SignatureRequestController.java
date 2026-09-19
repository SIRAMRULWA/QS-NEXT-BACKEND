package za.co.qsnext.employeemanagement.esignature;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.esignature.dto.CreateSignatureRequestRequest;
import za.co.qsnext.employeemanagement.esignature.dto.DeclineSignatureRequest;
import za.co.qsnext.employeemanagement.esignature.dto.SignatureRequestResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "E-Signature", description = "Document e-signature requests and signing.")
@RestController
@RequestMapping("/api/v1/esignature")
public class SignatureRequestController {

    private static final String MANAGE_AUTHORITY = "ESIGNATURE_MANAGE";

    private final SignatureRequestService signatureRequestService;

    public SignatureRequestController(SignatureRequestService signatureRequestService) {
        this.signatureRequestService = signatureRequestService;
    }

    @PreAuthorize("hasAuthority('ESIGNATURE_MANAGE')")
    @Operation(summary = "Create request")
    @PostMapping("/requests")
    public ResponseEntity<SignatureRequestResponse> createRequest(
            Authentication authentication,
            @Valid @RequestBody CreateSignatureRequestRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        SignatureRequestResponse response = signatureRequestService.createRequest(
                request.documentId(), request.title(), request.signerUserIds(),
                request.expiresAt(), userDetails.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('ESIGNATURE_MANAGE', 'ESIGNATURE_SIGN')")
    @Operation(summary = "Get by id")
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<SignatureRequestResponse> getById(
            Authentication authentication,
            @PathVariable UUID requestId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(signatureRequestService.getById(
                requestId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('ESIGNATURE_SIGN')")
    @Operation(summary = "Get my pending signatures")
    @GetMapping("/my-signatures")
    public ResponseEntity<List<SignatureRequestResponse>> getMyPendingSignatures(
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(signatureRequestService.getMyPendingSignatures(userDetails.getUserId()));
    }

    @PreAuthorize("hasAuthority('ESIGNATURE_SIGN')")
    @Operation(summary = "Accept")
    @PatchMapping("/signers/{signerId}/accept")
    public ResponseEntity<SignatureRequestResponse> accept(
            Authentication authentication,
            @PathVariable UUID signerId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(signatureRequestService.accept(signerId, userDetails.getUserId()));
    }

    @PreAuthorize("hasAuthority('ESIGNATURE_SIGN')")
    @Operation(summary = "Decline")
    @PatchMapping("/signers/{signerId}/decline")
    public ResponseEntity<SignatureRequestResponse> decline(
            Authentication authentication,
            @PathVariable UUID signerId,
            @Valid @RequestBody(required = false) DeclineSignatureRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String reason = request == null ? null : request.reason();

        return ResponseEntity.ok(signatureRequestService.decline(signerId, userDetails.getUserId(), reason));
    }

    @PreAuthorize("hasAuthority('ESIGNATURE_MANAGE')")
    @Operation(summary = "Cancel")
    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<SignatureRequestResponse> cancel(@PathVariable UUID requestId) {
        return ResponseEntity.ok(signatureRequestService.cancel(requestId));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
