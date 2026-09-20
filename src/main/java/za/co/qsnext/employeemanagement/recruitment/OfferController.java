package za.co.qsnext.employeemanagement.recruitment;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.recruitment.dto.CreateOfferRequest;
import za.co.qsnext.employeemanagement.recruitment.dto.HireRequest;
import za.co.qsnext.employeemanagement.recruitment.dto.HireResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.OfferResponse;

import java.util.UUID;

@Tag(name = "Recruitment - Offers", description = "Job offers and hiring.")
@RestController
@RequestMapping("/api/v1/recruitment/offers")
@PreAuthorize("hasAuthority('RECRUITMENT_MANAGE')")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @Operation(summary = "Create offer")
    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody CreateOfferRequest request) {
        OfferResponse response = offerService.createOffer(
                request.applicationId(), request.jobTitle(), request.salaryAmount(),
                request.currency(), request.startDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get offer")
    @GetMapping("/{offerId}")
    public ResponseEntity<OfferResponse> getOffer(@PathVariable UUID offerId) {
        return ResponseEntity.ok(offerService.getOffer(offerId));
    }

    @Operation(summary = "Send offer")
    @PatchMapping("/{offerId}/send")
    public ResponseEntity<OfferResponse> sendOffer(@PathVariable UUID offerId) {
        return ResponseEntity.ok(offerService.sendOffer(offerId));
    }

    @Operation(summary = "Accept offer")
    @PatchMapping("/{offerId}/accept")
    public ResponseEntity<OfferResponse> acceptOffer(@PathVariable UUID offerId) {
        return ResponseEntity.ok(offerService.acceptOffer(offerId));
    }

    @Operation(summary = "Decline offer")
    @PatchMapping("/{offerId}/decline")
    public ResponseEntity<OfferResponse> declineOffer(@PathVariable UUID offerId) {
        return ResponseEntity.ok(offerService.declineOffer(offerId));
    }

    @Operation(summary = "Withdraw offer")
    @PatchMapping("/{offerId}/withdraw")
    public ResponseEntity<OfferResponse> withdrawOffer(@PathVariable UUID offerId) {
        return ResponseEntity.ok(offerService.withdrawOffer(offerId));
    }

    @Operation(summary = "Hire")
    @PostMapping("/{offerId}/hire")
    public ResponseEntity<HireResponse> hire(
            @PathVariable UUID offerId,
            @Valid @RequestBody HireRequest request
    ) {
        HireResponse response = offerService.hire(
                offerId, request.username(), request.employeeNumber(), request.departmentId(),
                request.hireDate(), request.onboardingTemplateId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
