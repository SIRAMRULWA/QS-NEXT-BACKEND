package za.co.qsnext.employeemanagement.ai;

/**
 * The safe default when no AI vendor is configured (no API key present
 * at startup - see {@link AiProviderConfiguration}). QSNext must run
 * correctly with zero AI vendor configured; every AI capability simply
 * reports itself as unavailable rather than the application failing to
 * start or a request throwing an unhandled error.
 */
public class NoOpAiProvider implements AiProvider {

    @Override
    public String providerName() {
        return "none";
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "AI capabilities are not configured in this environment.";
    }
}
