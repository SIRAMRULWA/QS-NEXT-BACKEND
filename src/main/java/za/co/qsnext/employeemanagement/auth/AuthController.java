package za.co.qsnext.employeemanagement.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.auth.dto.*;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

@Tag(name = "Authentication", description = "Registration, login, JWT tokens, password reset and account security.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(summary = "Login")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        return ResponseEntity.ok(
                authService.login(request, httpRequest)
        );
    }

    @Operation(summary = "Register")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        authService.register(request, httpRequest)
                );
    }

    @Operation(summary = "Verify email address")
    @SecurityRequirements
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {

        emailVerificationService.verify(request.token());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resend email verification")
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        emailVerificationService.resend(userDetails.getUserId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Refresh")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {

        return ResponseEntity.ok(
                authService.refreshToken(request, httpRequest)
        );
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {

        authService.logout(request, extractBearerToken(httpRequest));

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                authService.getCurrentUser(
                        userDetails.getUserId(),
                        authentication.getAuthorities()
                )
        );
    }

    @Operation(summary = "Change password")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        authService.changePassword(
                userDetails.getUserId(),
                request,
                extractBearerToken(httpRequest)
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Forgot password")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        authService.forgotPassword(request);

        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Reset password")
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        authService.resetPassword(request);

        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(HttpServletRequest httpRequest) {

        String header = httpRequest.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        return header.substring("Bearer ".length());
    }
}
