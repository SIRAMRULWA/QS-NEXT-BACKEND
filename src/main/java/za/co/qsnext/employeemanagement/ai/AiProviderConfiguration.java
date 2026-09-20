package za.co.qsnext.employeemanagement.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses which {@link AiProvider} implementation the application runs
 * with, based purely on whether vendor credentials are present at
 * startup - never hardcoded. No API key configured means
 * {@link NoOpAiProvider}, so QSNext boots and runs correctly with zero
 * AI vendor set up. This is independent of the {@code
 * integration_configs} AI row's enabled flag, which {@code AiService}
 * checks separately at request time - the row is the business decision
 * to switch the feature on or off; this class is the infrastructure
 * decision of whether real credentials exist at all.
 * <p>
 * Per the project's secrets policy, the API key comes only from an
 * environment variable ({@code ANTHROPIC_API_KEY}), never from the
 * database.
 */
@Configuration
public class AiProviderConfiguration {

    @Bean
    public AiProvider aiProvider(
            @Value("${ANTHROPIC_API_KEY:}") String apiKey,
            @Value("${qsnext.ai.model:claude-opus-5}") String model
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return new NoOpAiProvider();
        }

        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        return new AnthropicAiProvider(client, model);
    }
}
