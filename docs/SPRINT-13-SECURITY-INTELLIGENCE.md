# Sprint 13 — Security Intelligence Foundation

## Objetivo

Evoluir o TenantGuard para uma camada de **Security Intelligence** capaz de analisar eventos de segurança e auditoria por tenant, preparando o projeto para integração segura com LLMs, RAG, Tool Calling e MCP sem acoplar a aplicação a um provedor específico nesta primeira etapa.

A Sprint 13 parte da base já existente no TenantGuard: autenticação JWT, RBAC, isolamento multi-tenant, PostgreSQL RLS, auditoria, eventos de segurança, Redis rate limiting, logs estruturados e observabilidade.

## Princípios arquiteturais

1. **Tenant isolation first** — nenhuma análise pode misturar dados entre tenants.
2. **Provider agnostic** — a camada de domínio não deve depender diretamente de OpenAI, Anthropic, Bedrock ou outro provedor.
3. **Least privilege** — o módulo de inteligência somente recebe os eventos permitidos ao tenant autenticado.
4. **Explainable output** — respostas devem indicar evidências e fatores utilizados na análise.
5. **No secret leakage** — prompts, logs e respostas não devem incluir tokens, senhas ou secrets.
6. **Human-in-the-loop** — o módulo inicialmente recomenda e explica; não executa ações destrutivas automaticamente.

## Arquitetura alvo da Sprint

```text
Authenticated ADMIN
       │
       ▼
/api/admin/security-intelligence
       │
       ▼
SecurityIntelligenceService
       │
       ├── TenantContext
       ├── Audit/Security Events
       ├── SecuritySignalExtractor
       └── SecurityAnalysisProvider
                │
                ├── deterministic/local baseline
                └── LLM adapter (next stage)
```

## Entregas

### Etapa 1 — Foundation

- criar pacote `securityintelligence`
- definir request/response records
- definir `SecurityAnalysisProvider` como porta de integração
- criar baseline determinístico sem dependência externa
- garantir que o tenant venha exclusivamente do contexto autenticado
- adicionar testes unitários da análise baseline

### Etapa 2 — Tenant-aware event query

- consultar eventos de segurança/auditoria apenas do tenant autenticado
- aceitar janela temporal controlada
- limitar quantidade de eventos por análise
- impedir tenant id informado pelo cliente

### Etapa 3 — Admin API

Endpoint planejado:

```http
POST /api/admin/security-intelligence/analyze
Authorization: Bearer <ADMIN_ACCESS_TOKEN>
Content-Type: application/json
```

Exemplo de request:

```json
{
  "lookbackHours": 24,
  "question": "Existem sinais de abuso de autenticação ou tentativa de acesso indevido?"
}
```

Exemplo conceitual de response:

```json
{
  "tenantId": "TENANT_A",
  "riskLevel": "MEDIUM",
  "summary": "Foram observadas múltiplas falhas de autenticação e eventos de autorização negada.",
  "signals": [
    "AUTH_FAILURE_SPIKE",
    "FORBIDDEN_ACCESS"
  ],
  "recommendations": [
    "Revisar origem das tentativas de autenticação",
    "Correlacionar eventos por correlationId e traceId"
  ]
}
```

## Etapa 4 — LLM adapter

Somente após a foundation estar testada:

- integrar Spring AI ou adapter equivalente
- externalizar configuração do modelo
- proteger prompt contra vazamento cross-tenant
- implementar timeout, retry e fallback
- adicionar métricas de latência, erro e uso do provider

## Etapa 5 — RAG / Tool Calling

Evolução planejada após o adapter LLM:

- RAG sobre runbooks e documentação de segurança
- Vector Store
- Tool Calling para consultas read-only
- MCP para integração padronizada de ferramentas
- guardrails para impedir ações privilegiadas não autorizadas

## Critérios de aceite da Sprint 13

- análise sempre usa o tenant autenticado
- nenhum `tenantId` é aceito do cliente
- USER não acessa endpoint ADMIN
- ADMIN recebe somente evidências do próprio tenant
- baseline funciona mesmo sem chave/API de LLM
- testes existentes continuam verdes
- novos testes cobrem tenant isolation e análise baseline
- logs da feature mantêm `correlationId`, `traceId` e `spanId`

## Fora de escopo nesta Sprint

- execução automática de bloqueios ou revogações por IA
- acesso cross-tenant administrativo
- treinamento/fine-tuning de modelo
- provisionamento de Vector DB gerenciado
- agente com permissão de escrita em produção

## Definition of Done

A Sprint 13 será considerada concluída quando o TenantGuard possuir um módulo de Security Intelligence tenant-aware, testável, provider-agnostic e preparado para receber um adapter LLM sem comprometer os controles de segurança já existentes.
