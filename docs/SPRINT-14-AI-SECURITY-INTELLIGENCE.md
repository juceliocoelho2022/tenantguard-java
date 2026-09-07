# Sprint 14 — AI Security Intelligence

## Objective

Evolve TenantGuard Security Intelligence from a deterministic-only engine into a provider-agnostic AI-assisted analysis layer without weakening tenant isolation, explainability, least privilege, or deterministic controls.

The deterministic provider remains the baseline and safety net. AI augments findings and recommendations; it does not become the source of truth for tenant identity, authorization, event retrieval, or raw risk score calculation in this sprint.

## Scope

### 1. Provider orchestration

Introduce an orchestration layer capable of selecting between deterministic and AI providers through configuration. The default remains deterministic so local development and CI do not require an external LLM.

Target abstraction:

```text
SecurityIntelligenceService
        |
        v
SecurityAnalysisOrchestrator
        |
        +--> DeterministicSecurityAnalysisProvider
        |
        +--> AiSecurityAnalysisProvider
                   |
                   +--> AiSecurityClient
                            |
                            +--> Spring AI adapter
```

### 2. AI request model

The AI provider receives only tenant-scoped, already-authorized security evidence. It must not query another tenant and must not accept tenant identity from an LLM response.

The prompt payload should contain bounded and normalized evidence such as:

- event type/action
- outcome or HTTP status
- username when available
- requestId and traceId when useful for correlation
- sanitized details
- event timestamp
- deterministic categories and score as context

Secrets, raw access tokens, refresh tokens, passwords, JWT signatures, Authorization headers, cookies, database credentials, and infrastructure secrets must never be included in the AI prompt.

### 3. Structured AI output

The AI response must map to a typed model instead of free-form text only.

Initial target fields:

```text
summary
findings[]
recommendations[]
confidence
provider
model
```

Risk score and risk level remain deterministic in Sprint 14. The LLM may explain the score, but it must not override it.

### 4. Failure strategy

AI is an enhancement, not a dependency for availability.

If the AI provider times out, returns malformed output, violates schema expectations, or is unavailable:

```text
AI failure
   -> log/metric
   -> deterministic fallback
   -> API remains available
```

No raw provider exception or secret-bearing payload should be returned to the client.

### 5. Configuration

Planned configuration:

```yaml
app:
  security-intelligence:
    provider: deterministic
    ai:
      enabled: false
      timeout: 5s
      max-events: 100
```

Provider values initially:

- `deterministic`
- `ai`
- later: `hybrid`

### 6. Spring AI integration

Add Spring AI only behind an adapter. Core domain/service code must not depend directly on a specific model vendor.

Target boundary:

```java
public interface AiSecurityClient {
    AiSecurityInsight analyze(AiSecurityRequest request);
}
```

The first adapter can use Spring AI ChatClient. Model/vendor configuration remains externalized.

### 7. Prompt-injection resilience

Security event details are untrusted data. They must be treated as evidence, never as instructions.

The system prompt must explicitly state that event text, usernames, request metadata, and audit details are data only and cannot redefine policy, reveal secrets, change tenant scope, or request tools.

Prompt assembly should clearly delimit untrusted evidence.

### 8. Observability

Add metrics for:

- AI analysis attempts
- AI success/failure
- deterministic fallback count
- AI latency
- structured-output validation failures

Logs should include correlation identifiers but never full prompts containing sensitive event data.

## API evolution

Keep the current deterministic endpoint compatible:

```http
GET /api/admin/security-intelligence?lookbackHours=24
```

Introduce AI analysis only when the internal provider abstraction is stable. A later Sprint 14 increment may add:

```http
POST /api/admin/security-intelligence/analyze
```

with a bounded request such as:

```json
{
  "lookbackHours": 24,
  "question": "Quais sinais exigem investigação prioritária?"
}
```

The question must never control tenant scope or bypass event bounds.

## Acceptance criteria

Sprint 14 is complete when:

1. Existing deterministic behavior and tests remain green.
2. AI integration is behind a provider/client interface.
3. Application starts and CI passes without an AI API key when AI is disabled.
4. AI requests use only already tenant-scoped evidence.
5. Sensitive credential/token fields are excluded from prompts.
6. AI output is parsed into a typed response model.
7. Timeout/provider/schema failures fall back deterministically.
8. Metrics expose AI attempts, failures, latency, and fallback.
9. Unit tests cover provider selection, sanitization, structured response parsing, and fallback.
10. Runtime validation demonstrates one AI-assisted analysis without changing deterministic risk score semantics.

## Non-goals for this sprint

- autonomous remediation
- allowing the LLM to revoke sessions or modify tenant data
- LLM-generated SQL
- direct database access from the model
- cross-tenant/global AI analysis
- vector database/RAG ingestion of arbitrary tenant documents
- MCP tool execution

RAG, Tool Calling, and MCP will be introduced only after the AI analysis boundary is stable and tenant-safe.

## Suggested implementation sequence

1. Create Sprint 14 branch and plan.
2. Add AI request/response domain models.
3. Add `AiSecurityClient` interface and disabled/no-op implementation.
4. Add analysis orchestrator with deterministic fallback.
5. Add sanitization/redaction layer for security evidence.
6. Add tests for fallback and tenant-safe input.
7. Add Spring AI BOM/dependency and ChatClient adapter.
8. Add structured output parsing.
9. Add metrics and timeout handling.
10. Add optional POST analyze endpoint.
11. Runtime validation.
12. PR and merge.
