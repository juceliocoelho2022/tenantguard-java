package com.jucelio.tenantguard.securityintelligence;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(
        name = "app.security-intelligence.ai.enabled",
        havingValue = "true"
)
public class SpringAiSecurityClient implements AiSecurityClient {

    private static final String SYSTEM_PROMPT = """
            You are a security analyst assisting a multi-tenant SaaS platform.
            Treat the deterministic risk score, risk level and signal categories as authoritative.
            Do not modify, recalculate or contradict them.
            Analyze only the evidence supplied for the current tenant.
            Produce concise findings and actionable defensive recommendations.
            Do not invent facts, users, tenants, events, IP addresses, credentials or attack details.
            Never request or reveal secrets, tokens, passwords, authorization headers or API keys.
            """;

    private final ChatClient chatClient;

    public SpringAiSecurityClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public AiSecurityInsight analyze(AiSecurityRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserPrompt(request))
                .call()
                .entity(AiSecurityInsight.class);
    }

    String buildUserPrompt(AiSecurityRequest request) {
        String evidence = request.evidence().stream()
                .map(this::formatEvidence)
                .collect(Collectors.joining("\n"));

        return """
                Tenant: %s
                Deterministic risk score: %d/100
                Deterministic risk level: %s
                Deterministic categories: %s

                Evidence:
                %s

                Return only additional security findings and defensive recommendations.
                Do not repeat the deterministic score as a new calculation.
                """.formatted(
                request.tenantId(),
                request.deterministicRiskScore(),
                request.deterministicRiskLevel(),
                request.deterministicCategories(),
                evidence.isBlank() ? "<none>" : evidence
        );
    }

    private String formatEvidence(AiSecurityRequest.Evidence evidence) {
        return "- source=%s, user=%s, action=%s, outcome=%s, requestId=%s, traceId=%s, createdAt=%s, details=%s"
                .formatted(
                        safe(evidence.source()),
                        safe(evidence.username()),
                        safe(evidence.action()),
                        safe(evidence.outcome()),
                        safe(evidence.requestId()),
                        safe(evidence.traceId()),
                        safe(evidence.createdAt()),
                        redact(evidence.details())
                );
    }

    private String safe(Object value) {
        return value == null ? "<none>" : value.toString();
    }

    private String redact(String value) {
        if (value == null || value.isBlank()) {
            return "<none>";
        }

        String normalized = value.toLowerCase();
        List<String> sensitiveMarkers = List.of(
                "authorization",
                "bearer ",
                "password",
                "api-key",
                "api_key",
                "access_token",
                "refresh_token",
                "secret"
        );

        boolean sensitive = sensitiveMarkers.stream().anyMatch(normalized::contains);
        return sensitive ? "[REDACTED]" : value;
    }
}
