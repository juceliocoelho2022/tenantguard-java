# Sprint 15 — Security Incident Response

## Objetivo

Transformar o TenantGuard de uma plataforma que apenas detecta e analisa sinais de segurança em uma plataforma capaz de **abrir, acompanhar e tratar incidentes de forma controlada, auditável e tenant-aware**.

A análise determinística continua sendo a autoridade para risco. A IA permanece restrita à explicação e enriquecimento narrativo. Nenhuma ação bloqueante deve ser executada apenas por recomendação do modelo.

## Resultado esperado

Ao final do Sprint 15, o TenantGuard deverá:

- converter análises de risco relevantes em incidentes de segurança;
- evitar incidentes duplicados para a mesma causa dentro de uma janela configurável;
- manter lifecycle explícito de incidente;
- preservar isolamento por tenant em banco e API;
- registrar trilha de auditoria das mudanças de estado;
- expor métricas operacionais para incidentes;
- permitir operação segura quando a IA estiver indisponível;
- manter decisões de abertura/prioridade baseadas em regras determinísticas.

## Arquitetura alvo

```text
Security Events / Audit Events
            |
            v
Security Intelligence
(deteministic risk authority)
            |
            v
Incident Policy Engine
            |
            +---- LOW ----> no incident
            |
            +---- MEDIUM/HIGH/CRITICAL
                        |
                        v
               Incident Service
                        |
              +---------+---------+
              |                   |
              v                   v
       PostgreSQL + RLS       Audit Trail
              |
              v
       Admin Incident API
              |
              v
      Metrics / Prometheus
```

## Escopo 15.1 — Modelo de domínio

Criar pacote `securityincident` com:

- `SecurityIncident`
- `SecurityIncidentStatus`
- `SecurityIncidentSeverity`
- `SecurityIncidentRepository`
- `SecurityIncidentService`

### Estados

```text
OPEN -> INVESTIGATING -> RESOLVED
  \          |
   \         v
    +------> DISMISSED
```

Transições inválidas devem retornar erro de domínio.

### Severidade

- LOW
- MEDIUM
- HIGH
- CRITICAL

A severidade inicial deve derivar deterministicamente do `riskScore` / `riskLevel` existente.

## Escopo 15.2 — Persistência tenant-aware

Nova tabela Flyway `security_incident` contendo, no mínimo:

- `id`
- `tenant_id`
- `fingerprint`
- `severity`
- `status`
- `risk_score`
- `title`
- `summary`
- `source_category`
- `first_seen_at`
- `last_seen_at`
- `created_at`
- `updated_at`
- `resolved_at`
- `version`

Requisitos:

- RLS obrigatória;
- `tenant_id` nunca vem do cliente;
- índice por `tenant_id`, `status`, `severity`;
- índice único/estratégia equivalente para deduplicação por fingerprint;
- optimistic locking via `version`.

## Escopo 15.3 — Incident Policy Engine

Criar uma camada determinística que decide se uma análise deve virar incidente.

Exemplo inicial:

```text
LOW      -> não abre incidente
MEDIUM   -> abre incidente MEDIUM quando houver sinal acionável
HIGH     -> abre incidente HIGH
CRITICAL -> abre incidente CRITICAL
```

O policy engine deve receber `SecurityAnalysis` e retornar uma decisão explícita, por exemplo:

```java
IncidentDecision(
    boolean createIncident,
    SecurityIncidentSeverity severity,
    String reason
)
```

A decisão não pode depender de texto gerado pela IA.

## Escopo 15.4 — Deduplicação / fingerprint

Incidentes equivalentes não devem gerar avalanche de registros.

Gerar fingerprint estável utilizando campos determinísticos, por exemplo:

```text
tenantId + primaryCategory + affectedUser + normalizedAction
```

A estratégia final deve ser testável e documentada.

Comportamento esperado:

- primeiro sinal relevante -> cria incidente;
- novo sinal equivalente dentro da janela -> atualiza `last_seen_at` e contadores;
- sinal diferente -> novo incidente.

## Escopo 15.5 — API administrativa

Endpoints sugeridos:

```text
GET    /api/admin/security-incidents
GET    /api/admin/security-incidents/{id}
PATCH  /api/admin/security-incidents/{id}/status
```

Filtros:

- status
- severity
- período

Regras:

- apenas ADMIN;
- tenant derivado do JWT;
- acesso cross-tenant retorna 404;
- payloads não expõem detalhes sensíveis desnecessários.

## Escopo 15.6 — Auditoria

Registrar mudanças de lifecycle:

- incidente criado;
- status alterado;
- incidente resolvido;
- incidente descartado.

Campos mínimos:

```text
tenantId
incidentId
actor
action
oldStatus
newStatus
requestId
traceId
timestamp
```

## Escopo 15.7 — Observabilidade

Métricas Micrometer:

```text
tenantguard.security.incidents.created
tenantguard.security.incidents.updated
tenantguard.security.incidents.resolved
tenantguard.security.incidents.dismissed
tenantguard.security.incidents.open
```

Evitar `tenantId` como tag para não criar alta cardinalidade.

Tags permitidas quando úteis:

- severity
- status

## Escopo 15.8 — Testes

Cobertura mínima:

- policy engine LOW não abre incidente;
- HIGH abre incidente;
- severidade é derivada deterministicamente;
- incidente duplicado é consolidado;
- transição válida de estado funciona;
- transição inválida é rejeitada;
- USER recebe 403;
- ADMIN acessa incidente do próprio tenant;
- cross-tenant retorna 404;
- RLS impede acesso indevido;
- métricas são incrementadas corretamente.

Meta inicial do Sprint 15:

```text
>= 70 testes totais
0 failures
0 errors
```

A quantidade é secundária à cobertura dos invariantes de segurança.

## Fora do escopo inicial

Para evitar excesso de acoplamento neste sprint:

- Kafka / event streaming externo;
- notificações Slack/e-mail;
- bloqueio automático de usuário;
- revogação automática de sessão por IA;
- SOAR externo;
- webhook de terceiros.

Esses itens podem compor o Sprint 16 após o lifecycle de incidentes estar estável.

## Definition of Done

Sprint 15 só pode ser concluído quando:

- migrations passam do zero e em banco existente;
- testes Maven passam localmente;
- CI GitHub Actions passa;
- endpoint tenant-aware é validado em runtime;
- cross-tenant é validado em runtime;
- nenhuma decisão crítica depende de saída do LLM;
- logs não vazam secrets ou payloads sensíveis;
- documentação é atualizada;
- PR é revisado antes do merge em `main`.
