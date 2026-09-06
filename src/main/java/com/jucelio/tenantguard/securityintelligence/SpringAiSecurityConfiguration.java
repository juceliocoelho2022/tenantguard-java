package com.jucelio.tenantguard.securityintelligence;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "app.security-intelligence.ai.enabled",
        havingValue = "true"
)
public class SpringAiSecurityConfiguration {

    @Bean
    ChatClient.Builder securityIntelligenceChatClientBuilder(
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-4o-mini}") String model,
            @Value("${OPENAI_TEMPERATURE:0.2}") Double temperature,
            @Value("${app.security-intelligence.ai.timeout:5s}") Duration timeout
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY must be set when app.security-intelligence.ai.enabled=true"
            );
        }

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("AI timeout must be greater than zero");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel);
    }
}
