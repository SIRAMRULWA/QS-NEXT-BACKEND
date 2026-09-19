package za.co.qsnext.employeemanagement.mobile;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.mobile.dto.MobileDashboardResponse;
import za.co.qsnext.employeemanagement.mobile.dto.MobileDeviceResponse;
import za.co.qsnext.employeemanagement.mobile.dto.RegisterDeviceRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {

    private final MobileService mobileService;

    public MobileController(MobileService mobileService) {
        this.mobileService = mobileService;
    }

    @PreAuthorize("""
            hasAuthority('EMPLOYEE_READ')
            and hasAuthority('ATTENDANCE_READ')
            and hasAuthority('LEAVE_READ')
            and hasAuthority('CALENDAR_READ')
            and hasAuthority('NOTIFICATION_READ')
            """)
    @GetMapping("/dashboard")
    public ResponseEntity<MobileDashboardResponse> getDashboard(Authentication authentication) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(mobileService.getDashboard(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/devices")
    public ResponseEntity<MobileDeviceResponse> registerDevice(
            Authentication authentication,
            @Valid @RequestBody RegisterDeviceRequest request
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                mobileService.registerDevice(userId, request.deviceToken(), request.platform()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/devices")
    public ResponseEntity<List<MobileDeviceResponse>> getMyDevices(Authentication authentication) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(mobileService.getMyDevices(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Void> unregisterDevice(
            Authentication authentication,
            @PathVariable UUID deviceId
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        mobileService.unregisterDevice(userId, deviceId);
        return ResponseEntity.noContent().build();
    }
}
