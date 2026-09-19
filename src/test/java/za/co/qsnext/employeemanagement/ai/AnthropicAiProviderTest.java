package za.co.qsnext.employeemanagement.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.services.blocking.MessageService;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnthropicAiProviderTest {

    @Test
    void providerName_isAnthropic() {
        AnthropicAiProvider provider = new AnthropicAiProvider(mock(AnthropicClient.class), "claude-opus-5");

        assertThat(provider.providerName()).isEqualTo("anthropic");
    }

    @Test
    void complete_wrapsAVendorFailure_inAiProviderException() {
        AnthropicClient client = mock(AnthropicClient.class);
        MessageService messageService = mock(MessageService.class);

        when(client.messages()).thenReturn(messageService);
        when(messageService.create(any(MessageCreateParams.class)))
                .thenThrow(new AnthropicException("simulated vendor outage"));

        AnthropicAiProvider provider = new AnthropicAiProvider(client, "claude-opus-5");

        assertThatThrownBy(() -> provider.complete("system prompt", "user prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasCauseInstanceOf(AnthropicException.class);
    }
}
