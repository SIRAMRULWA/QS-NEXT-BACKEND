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

    public String renderBody(Map<String, String> variables) {

        String rendered = bodyTemplate;

        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace(
                    "{{" + variable.getKey() + "}}",
                    variable.getValue()
            );
        }

        return rendered;
    }
}
