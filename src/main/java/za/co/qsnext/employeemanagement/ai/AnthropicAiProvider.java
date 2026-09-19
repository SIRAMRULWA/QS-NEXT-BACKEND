package za.co.qsnext.employeemanagement.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import java.util.stream.Collectors;

/**
 * The concrete Anthropic implementation of {@link AiProvider}, wired up
 * only when an API key is configured - see {@link AiProviderConfiguration}.
 * A vendor client failure never propagates as a raw SDK exception; it is
 * wrapped in {@link AiProviderException} so callers only ever depend on
 * this module's own exception type.
 */
public class AnthropicAiProvider implements AiProvider {

    private static final long MAX_RESPONSE_TOKENS = 2048L;

    private final AnthropicClient client;
    private final String model;

    public AnthropicAiProvider(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public String providerName() {
        return "anthropic";
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(MAX_RESPONSE_TOKENS)
                    .system(systemPrompt)
                    .addUserMessage(userPrompt)
                    .build();

            Message response = client.messages().create(params);

            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining());
        } catch (AnthropicException exception) {
            throw new AiProviderException("Anthropic AI provider request failed", exception);
        }
    }
}
