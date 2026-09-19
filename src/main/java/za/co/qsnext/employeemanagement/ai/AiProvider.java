package za.co.qsnext.employeemanagement.ai;

/**
 * The SAFE abstraction boundary between the rest of the application and
 * any specific AI vendor. Every AI-powered capability in {@code
 * AiService} goes through this single-method interface rather than
 * calling a vendor SDK directly, so QSNext is never hard-wired to one
 * provider - {@link AnthropicAiProvider} is the only implementation
 * today, but an OpenAI or other provider implementation can be added
 * later without touching {@code AiService} or any controller.
 * <p>
 * A provider implementation must not throw for ordinary "the model
 * declined" outcomes - it should return an explanatory string. It
 * should throw {@link AiProviderException} only for genuine
 * infrastructure failures (network error, authentication failure, rate
 * limiting) that the caller cannot recover from.
 */
public interface AiProvider {

    /**
     * Returns the provider/vendor name this implementation talks to,
     * for display and audit purposes (e.g. "anthropic", "none").
     */
    String providerName();

    /**
     * Requests a single text completion. Implementations are free to
     * choose their own model, token limits and request shape - callers
     * only see the resulting text.
     */
    String complete(String systemPrompt, String userPrompt);
}
