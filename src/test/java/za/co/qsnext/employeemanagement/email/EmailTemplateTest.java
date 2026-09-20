package za.co.qsnext.employeemanagement.email;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateTest {

    @Test
    void renderBody_substitutesAllPlaceholders() {
        String body = EmailTemplate.PASSWORD_RESET.renderBody(Map.of(
                "token", "abc123",
                "expiresInMinutes", "30"
        ));

        assertThat(body).contains("abc123");
        assertThat(body).contains("30 minutes");
        assertThat(body).doesNotContain("{{");
    }

    @Test
    void renderBody_leavesUnmatchedPlaceholdersAlone_whenAVariableIsMissing() {
        String body = EmailTemplate.PASSWORD_RESET.renderBody(Map.of("token", "abc123"));

        assertThat(body).contains("abc123");
        assertThat(body).contains("{{expiresInMinutes}}");
    }

    @Test
    void everyTemplate_hasANonBlankTypeAndSubject() {
        for (EmailTemplate template : EmailTemplate.values()) {
            assertThat(template.type()).isNotBlank();
            assertThat(template.subject()).isNotBlank();
        }
    }
}
