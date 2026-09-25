package za.co.qsnext.employeemanagement.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.qsnext.employeemanagement.user.dto.UserResponse;

import java.util.UUID;

@Tag(name = "Users", description = "User accounts, roles and permissions.")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Search by username or email")
    @GetMapping("/search")
    public ResponseEntity<Page<UserResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        Page<UserResponse> response =
                userService.search(query, pageable)
                        .map(UserResponse::from);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get by id")
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
    @Operation(summary = "Get by username")
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
    @Operation(summary = "Disable")
    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Void> disable(
            @PathVariable UUID userId
    ) {

        userService.disable(userId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('USER_ENABLE')")
    @Operation(summary = "Enable")
    @PatchMapping("/{userId}/enable")
    public ResponseEntity<Void> enable(
            @PathVariable UUID userId
    ) {

        userService.enable(userId);

        return ResponseEntity.noContent().build();
    }

    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        return PageRequest.of(page, size);
    }
}