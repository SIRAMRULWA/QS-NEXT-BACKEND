package za.co.qsnext.employeemanagement.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpAiProviderTest {

    @Test
    void complete_returnsAnExplanatoryMessage_neverThrows() {
        NoOpAiProvider provider = new NoOpAiProvider();

        assertThat(provider.providerName()).isEqualTo("none");
        assertThat(provider.complete("system", "question")).contains("not configured");
    }
}
