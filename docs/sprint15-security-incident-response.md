# Sprint 15 — Security Incident Response

## Objetivo

Transformar o TenantGuard de uma plataforma que detecta e analisa sinais de segurança em uma plataforma capaz de **abrir, acompanhar e tratar incidentes de forma controlada, auditável e tenant-aware**.

A análise determinística continua sendo a autoridade para risco. A IA permanece restrita à explicação e ao enriquecimento narrativo. Nenhuma ação crítica ou bloqueante depende exclusivamente de recomendação do modelo.

## Status da implementação

O núcleo funcional planejado para o Sprint 15 foi implementado na branch `feat/sprint15-security-incident-response`.

Validação local mais recente em 07/09/2026:

```text
Tests run: 90
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A conclusão formal do sprint ainda depende dos gates restantes da Definition of Done: CI da branch/PR, validação runtime dos endpoints tenant-aware e cross-tenant, revisão do PR e merge em `main`.

## Arquitetura implementada

```text
Security Events / Audit Events
            |
            v
Security Intelligence
(deterministic risk authority)
            |
            v
Incident Policy Engine
            |
            +---- LOW ----> no incident
            |
            +---- actionable risk
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

## 15.1 — Modelo de domínio — concluído

Pacote `securityincident` com:

- `SecurityIncident`;
- `SecurityIncidentStatus`;
- `SecurityIncidentSeverity`;
- `SecurityIncidentDecision`;
- `SecurityIncidentPolicy`;
- `SecurityIncidentService`;
- `SecurityIncidentRepository`.

Lifecycle explícito:

```text
OPEN -> INVESTIGATING -> RESOLVED
  \          |
   \         v
    +------> DISMISSED
```

Transições inválidas são rejeitadas pelo domínio.

Severidades:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

A classificação é determinística e baseada no score e nos sinais produzidos pelo Security Intelligence.

## 15.2 — Persistência tenant-aware — concluído

A entidade `SecurityIncident` é persistida em PostgreSQL com JPA e optimistic locking via `@Version`.

As migrations do Sprint 15 criam `security_incidents`, índices operacionais, RLS e a restrição de deduplicação ativa.

Controles implementados:

- `tenant_id` não é fornecido pelo cliente;
- RLS com `ENABLE ROW LEVEL SECURITY` e `FORCE ROW LEVEL SECURITY`;
- policy vinculada ao `app.current_tenant`;
- acesso pelo role `tenantguard_app`;
- optimistic locking;
- índices tenant-aware;
- testes Testcontainers para isolamento e rejeição cross-tenant.

## 15.3 — Incident Policy Engine — concluído

`SecurityIncidentPolicy` recebe `SecurityAnalysis` e decide deterministicamente se um incidente deve ser aberto.

Regras atuais incluem threshold de score e tratamento explícito de `TOKEN_REPLAY`. A severidade do incidente é derivada pelo backend.

**Invariante:** texto produzido por LLM não participa da decisão de abertura, classificação ou lifecycle do incidente.

## 15.4 — Deduplicação e fingerprint — concluído

O fingerprint foi isolado em `SecurityIncidentFingerprint` e usa apenas atributos estáveis disponíveis no modelo atual:

```text
tenantId + categorias de sinal ordenadas
```

O SHA-256 resultante não depende de:

- `riskScore`;
- `riskLevel`;
- janela temporal;
- timestamps;
- findings;
- recommendations;
- texto produzido pela IA.

Testes comprovam que a ordem das categorias e mudanças de score/risk level não alteram o fingerprint, enquanto tenants diferentes produzem fingerprints diferentes.

A tabela também possui índice único parcial para impedir mais de um incidente ativo com o mesmo `(tenant_id, fingerprint)` nos estados `OPEN` e `INVESTIGATING`.

### Limitação conhecida

O modelo `SecurityAnalysis` atual não possui `affectedUser` ou `normalizedAction`. Por isso, o fingerprint por categorias é deliberadamente mais amplo. Uma evolução futura pode aumentar a granularidade quando esses atributos fizerem parte do sinal determinístico.

O índice único protege a integridade contra duplicatas ativas. O fluxo de aplicação ainda utiliza consulta seguida de persistência; portanto, tratamento transparente de corrida concorrente via `INSERT ... ON CONFLICT` não faz parte desta entrega e não deve ser confundido com deduplicação totalmente lock-free.

## 15.5 — API administrativa — concluído

Endpoints implementados:

```text
GET   /api/admin/security-incidents
GET   /api/admin/security-incidents/{id}
PATCH /api/admin/security-incidents/{id}/investigate
PATCH /api/admin/security-incidents/{id}/resolve
PATCH /api/admin/security-incidents/{id}/dismiss
```

Regras:

- acesso administrativo protegido por RBAC;
- tenant derivado do JWT/TenantContext;
- tenant não é aceito no payload;
- recurso cross-tenant é tratado como não encontrado;
- resposta não expõe `tenantId`;
- resolução/descartes aceitam nota validada sem inseri-la como tag de métrica.

## 15.6 — Auditoria — concluído

`SecurityIncidentAuditService` centraliza eventos de lifecycle:

```text
SECURITY_INCIDENT_OPENED
SECURITY_INCIDENT_INVESTIGATION_STARTED
SECURITY_INCIDENT_RESOLVED
SECURITY_INCIDENT_DISMISSED
```

A auditoria registra o incidente como recurso e reutiliza o contexto autenticado, tenant, request/trace correlation e timestamp do mecanismo existente de audit trail.

Notas de resolução e payloads sensíveis não são copiados para o evento de auditoria.

Existe teste de integração provando o caminho HTTP -> Security -> Incident Service -> Audit persistence.

## 15.7 — Observabilidade — concluído

Métricas Micrometer implementadas:

```text
tenantguard.security.incidents.opened
tenantguard.security.incidents.deduplicated
tenantguard.security.incidents.investigation.started
tenantguard.security.incidents.resolved
tenantguard.security.incidents.dismissed
tenantguard.security.incidents.closure.duration
```

Tags são limitadas a dimensões de baixa cardinalidade, como `severity` e `outcome`.

Não são usadas como tags:

- tenantId;
- incidentId;
- fingerprint;
- username;
- resolution note.

Não foi criado gauge process-local de incidentes ativos, pois ele poderia representar incorretamente o estado global em reinícios ou múltiplas réplicas. Uma métrica desse tipo deve ser derivada de fonte persistente com semântica multi-tenant adequada.

## 15.8 — Testes — concluído localmente

Cobertura inclui:

- LOW sem abertura de incidente;
- threshold e `TOKEN_REPLAY`;
- severidade determinística;
- fingerprint estável;
- deduplicação ativa;
- lifecycle válido e inválido;
- optimistic locking/persistência;
- RLS e rejeição cross-tenant;
- `401`, `403`, `404`, `400` e `409` nos cenários relevantes;
- API administrativa;
- persistência da auditoria;
- contadores e timer Micrometer;
- integração com PostgreSQL via Testcontainers.

Meta inicial:

```text
>= 70 testes
0 failures
0 errors
```

Resultado local atual:

```text
90 testes
0 failures
0 errors
0 skipped
BUILD SUCCESS
```

## Fora do escopo inicial

Permanecem fora desta entrega:

- Kafka / event streaming externo;
- notificações Slack/e-mail;
- bloqueio automático de usuário;
- revogação automática de sessão por IA;
- SOAR externo;
- webhook de terceiros.

Esses itens podem compor o Sprint 16 após a consolidação do lifecycle de incidentes.

## Definition of Done

Status dos gates:

- [x] implementação das migrations;
- [x] suíte Maven completa passa localmente — 90/90;
- [x] RLS e cross-tenant cobertos por Testcontainers/MockMvc;
- [x] decisões críticas independem de saída do LLM;
- [x] métricas evitam identificadores de alta cardinalidade;
- [x] documentação atualizada;
- [ ] GitHub Actions CI confirmado para o head final;
- [ ] endpoint tenant-aware validado manualmente em runtime no head final;
- [ ] cross-tenant validado manualmente em runtime no head final;
- [ ] PR revisado;
- [ ] merge em `main`.

O Sprint 15 deve ser marcado como formalmente concluído somente após os gates pendentes acima.