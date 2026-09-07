package za.co.qsnext.employeemanagement.selfservice;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;

@RestController
@RequestMapping("/api/v1/self-service")
public class SelfServiceController {

    private final SelfServiceService selfServiceService;

    public SelfServiceController(
            SelfServiceService selfServiceService
    ) {
        this.selfServiceService = selfServiceService;
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    @GetMapping("/profile")
    public ResponseEntity<SelfServiceProfileResponse> getOwnProfile(
            Authentication authentication
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        SelfServiceProfileResponse response =
                selfServiceService.getOwnProfile(
                        userDetails.getUserId()
                );

        return ResponseEntity.ok(response);
    }
}