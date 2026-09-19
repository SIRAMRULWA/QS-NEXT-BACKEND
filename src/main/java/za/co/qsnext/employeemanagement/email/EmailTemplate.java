package za.co.qsnext.employeemanagement.email;

import java.util.Map;

/**
 * Registry of known email templates. Each template owns its {@code type}
 * (persisted on {@link Email}, e.g. for reporting), subject and a body
 * with {@code {{placeholder}}} variables filled in at queue time.
 */
public enum EmailTemplate {

    PASSWORD_RESET(
            "PASSWORD_RESET",
            "Reset your QSNext password",
            "A password reset was requested for your account. Use this token "
                    + "to reset your password: {{token}}. This token expires in "
                    + "{{expiresInMinutes}} minutes. If you did not request this, "
                    + "you can ignore this email."
    ),

    WELCOME(
            "WELCOME",
            "Welcome to QSNext",
            "Hi {{username}}, your QSNext account has been created. "
                    + "You can now sign in to access the employee portal."
    ),

    ACCOUNT_LOCKED(
            "ACCOUNT_LOCKED",
            "Your QSNext account has been locked",
            "Your account was locked after too many failed sign-in attempts. "
                    + "It will unlock automatically at {{lockedUntil}}. If this "
                    + "was not you, please reset your password as soon as it unlocks."
    ),

    /**
     * Generic carrier for any in-app {@code Notification} that a user has
     * also opted into receiving by email - see the {@code notification}
     * package. Uses the notification's own title/message rather than a
     * type-specific template, so a new notification type doesn't need a
     * matching email template before it can be emailed.
     */
    NOTIFICATION(
            "NOTIFICATION",
            "{{title}}",
            "{{message}}"
    ),

    /**
     * Candidate-facing (see the {@code recruitment} package) - candidates
     * have no User account, so this is queued directly to their email
     * address rather than via NotificationPublisher.
     */
    INTERVIEW_INVITATION(
            "INTERVIEW_INVITATION",
            "Interview invitation - {{jobTitle}}",
            "Hi {{candidateName}}, you have been invited to interview for "
                    + "{{jobTitle}} on {{scheduledAt}}{{locationSuffix}}. "
                    + "We look forward to speaking with you."
    ),

    OFFER_EXTENDED(
            "OFFER_EXTENDED",
            "Job offer - {{jobTitle}}",
            "Hi {{candidateName}}, we are pleased to offer you the position of "
                    + "{{jobTitle}} with a start date of {{startDate}}. "
                    + "Please reply to this email to let us know your decision."
    );

    private final String type;
    private final String subject;
    private final String bodyTemplate;

    EmailTemplate(String type, String subject, String bodyTemplate) {
        this.type = type;
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
    }

    public String type() {
        return type;
    }

    public String subject() {
        return subject;
    }

    public String renderSubject(Map<String, String> variables) {
        return render(subject, variables);
    }

    public String renderBody(Map<String, String> variables) {
        return render(bodyTemplate, variables);
    }

    private static String render(String template, Map<String, String> variables) {

        String rendered = template;

        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace(
                    "{{" + variable.getKey() + "}}",
                    variable.getValue()
            );
        }

        return rendered;
    }
}
