package za.co.qsnext.employeemanagement.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.qsnext.employeemanagement.security.SecurityUtils;
import za.co.qsnext.employeemanagement.user.dto.ChangePasswordRequest;
import za.co.qsnext.employeemanagement.user.dto.UserResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getById(
            @PathVariable UUID userId
    ) {

        User user = userService.getById(userId);

        return ResponseEntity.ok(
                UserResponse.from(user)
        );
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getByUsername(
            @PathVariable String username
    ) {

        User user = userService.getByUsername(username);

        return ResponseEntity.ok(
                UserResponse.from(user)
        );
    }

    @PreAuthorize("hasAuthority('USER_DISABLE')")
    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Void> disable(
            @PathVariable UUID userId
    ) {

        userService.disable(userId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('USER_ENABLE')")
    @PatchMapping("/{userId}/enable")
    public ResponseEntity<Void> enable(
            @PathVariable UUID userId
    ) {

        userService.enable(userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeOwnPassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        userService.changePassword(
                currentUserId,
                request.currentPassword(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }
}