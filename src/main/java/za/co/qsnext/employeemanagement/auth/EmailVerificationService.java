package za.co.qsnext.employeemanagement.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;
import za.co.qsnext.employeemanagement.user.UserService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Confirms that a self-signup applicant owns the email address they
 * registered with. Applying for a job is blocked until they do, because
 * an application is matched to any existing candidate record by email.
 */
@Service
public class EmailVerificationService {

    static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    private static final String ENTITY_TYPE_USER = "User";

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final AuditService auditService;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            UserService userService,
            EmailService emailService,
            AuditService auditService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public void sendVerification(User user) {

        if (user.isEmailVerified()) {
            return;
        }

        String rawToken = generateSecureToken();

        tokenRepository.save(new EmailVerificationToken(
                user.getId(),
                hashToken(rawToken),
                OffsetDateTime.now().plus(TOKEN_LIFETIME)
        ));

        emailService.queueEmail(
                EmailTemplate.VERIFY_EMAIL,
                user.getEmail(),
                Map.of(
                        "username", user.getUsername(),
                        "token", rawToken,
                        "expiresInHours", String.valueOf(TOKEN_LIFETIME.toHours())
                )
        );
    }

    @Transactional
    public void resend(UUID userId) {
        sendVerification(userService.getById(userId));
    }

    @Transactional
    public void verify(String rawToken) {

        OffsetDateTime now = OffsetDateTime.now();

        EmailVerificationToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new BusinessRuleException("This verification code is invalid or has expired"));

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessRuleException("This verification code is invalid or has expired"));

        token.markUsed(now);
        user.markEmailVerified();

        auditService.log(user.getId(), "EMAIL_VERIFIED", ENTITY_TYPE_USER, user.getId(), AuditService.RESULT_SUCCESS);
    }

    private String generateSecureToken() {

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
