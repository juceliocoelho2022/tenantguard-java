# 🛡️ TenantGuard Java

<p align="center">
  <a href="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml">
    <img src="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml/badge.svg?branch=main" alt="TenantGuard CI">
  </a>
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen" alt="Spring Boot 3.5">
  <img src="https://img.shields.io/badge/Spring%20AI-1.1.8-brightgreen" alt="Spring AI 1.1.8">
  <img src="https://img.shields.io/badge/PostgreSQL-RLS-blue" alt="PostgreSQL RLS">
  <img src="https://img.shields.io/badge/Security-RBAC-success" alt="RBAC">
  <img src="https://img.shields.io/badge/Tests-90%20passing-success" alt="90 tests passing locally on Sprint 15">
</p>

<p align="center">
  <img src="docs/images/tenantGuard-architecture.png" alt="TenantGuard Java - Secure Multi-Tenant Architecture" width="100%">
</p>

**TenantGuard Java** é uma Proof of Concept de backend SaaS multi-tenant construída com **Java 21 e Spring Boot**, evoluída com segurança em profundidade, isolamento por tenant, observabilidade, resiliência, infraestrutura como código, inteligência de segurança assistida por IA e resposta operacional a incidentes.

O tenant é derivado do JWT autenticado, propagado pelo `TenantContext`, aplicado nas consultas e reforçado pelo PostgreSQL através de **Row Level Security (RLS)**. A IA nunca substitui decisões determinísticas de segurança: `riskScore`, `riskLevel`, categorias, incident policy e lifecycle crítico continuam sob controle do backend.

> Projeto voltado a estudo, ensino, portfólio e demonstração arquitetural. A presença de controles de produção não significa que a PoC seja um SaaS comercial pronto para uso sem revisão operacional, segurança, custos, compliance e SRE.

---

## 🚀 Estado atual

| Marco | Status |
|---|---|
| Baseline v1 — multi-tenancy, RLS, JWT, observabilidade e AWS IaC | ✅ concluído |
| Sprint 13 — Security Intelligence determinística + token replay | ✅ concluído |
| Sprint 14 — Spring AI + fallback seguro + métricas | ✅ integrado à `main` |
| Sprint 15 — Security Incident Response | ✅ implementação e testes locais concluídos; CI/PR/runtime final pendentes |

Validação local mais recente do Sprint 15:

```text
Tests run: 90
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

---

## 🎯 Arquitetura

```text
Cliente
  │
  ▼
Spring Security + JWT
  │
  ▼
TenantContext + RBAC
  │
  ├──► Services / Repositories ──► PostgreSQL + RLS
  │
  ├──► Security / Audit Events
  │          │
  │          ▼
  │    Security Intelligence
  │      ├── Deterministic Risk Engine
  │      └── Spring AI enrichment ──► Safe Fallback
  │          │
  │          ▼
  │    Incident Policy Engine
  │          │
  │          ▼
  │    Security Incident Service
  │      ├── stable fingerprint / dedup
  │      ├── audit trail
  │      └── lifecycle metrics
  │
  ├──► Redis Rate Limiting
  │
  └──► Prometheus / Grafana / Loki / Tempo
```

---

## 🧠 Security Intelligence + Spring AI

O módulo de Security Intelligence produz avaliação determinística com `riskScore`, `riskLevel`, categorias, findings e recomendações.

Categorias atuais:

```text
AUTH_FAILURE
ACCESS_DENIED
RATE_LIMIT
TOKEN_REPLAY
GENERIC_FAILURE
```

O Spring AI pode enriquecer findings e recommendations, mas não altera score, risk level ou decisões críticas. Em timeout, erro, resposta nula ou indisponibilidade do provedor, o backend mantém o resultado determinístico. Evidência vazia também evita chamada externa desnecessária.

Métricas do fluxo de IA:

```text
tenantguard.security.intelligence.ai.attempts
tenantguard.security.intelligence.ai.successes
tenantguard.security.intelligence.ai.failures
tenantguard.security.intelligence.ai.fallbacks
tenantguard.security.intelligence.ai.latency
```

---

## 🚨 Security Incident Response — Sprint 15

```text
Security Events
      ↓
Security Intelligence
      ↓
Deterministic Incident Policy
      ↓
Stable Fingerprint + Active Dedup
      ↓
Security Incident
      ↓
OPEN → INVESTIGATING → RESOLVED
  └────────────────────→ DISMISSED
      ↓
Audit + Metrics + PostgreSQL RLS
```

Implementado:

- `SecurityIncident` como entidade JPA;
- lifecycle explícito e transições validadas;
- severidade `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`;
- policy determinística;
- fingerprint SHA-256 estável;
- serviço e repository tenant-aware;
- optimistic locking com `@Version`;
- Flyway V9/V10;
- PostgreSQL RLS + `FORCE ROW LEVEL SECURITY`;
- índice único parcial para incidentes ativos equivalentes;
- API administrativa protegida por RBAC;
- audit trail do lifecycle;
- métricas Micrometer;
- testes unitários, MockMvc e Testcontainers.

### Fingerprint estável

```text
tenantId + categorias ordenadas
```

O fingerprint não depende de score, risk level, timestamps, janela de análise, findings, recommendations ou texto de IA. O modelo atual ainda não possui `affectedUser`/`normalizedAction`; esses atributos poderão aumentar a granularidade futuramente.

### API administrativa

```text
GET   /api/admin/security-incidents
GET   /api/admin/security-incidents/{id}
PATCH /api/admin/security-incidents/{id}/investigate
PATCH /api/admin/security-incidents/{id}/resolve
PATCH /api/admin/security-incidents/{id}/dismiss
```

O tenant nunca é escolhido pelo cliente e não é exposto no DTO de resposta.

### Métricas

```text
tenantguard.security.incidents.opened
tenantguard.security.incidents.deduplicated
tenantguard.security.incidents.investigation.started
tenantguard.security.incidents.resolved
tenantguard.security.incidents.dismissed
tenantguard.security.incidents.closure.duration
```

As tags são de baixa cardinalidade. Tenant, incident ID, fingerprint, username e notas de resolução não são labels.

---

## 🚀 Stack

**Backend:** Java 21, Spring Boot 3.5, Spring Security, Spring AI 1.1.8, Spring Data JPA, PostgreSQL 17, Flyway e Redis.

**Segurança:** JWT access/refresh, RBAC, TenantContext, PostgreSQL RLS, refresh-token rotation/revocation, replay detection, rate limiting distribuído e audit trail.

**Qualidade:** JUnit 5, Mockito, MockMvc, Testcontainers e GitHub Actions.

**Observabilidade:** Actuator, Micrometer, Prometheus, Grafana, logs JSON, Loki/Promtail, OpenTelemetry e Tempo.

**Infraestrutura:** Docker/Compose, Kubernetes, HPA, Terraform, AWS EKS, RDS PostgreSQL, ElastiCache Redis, ECR, Secrets Manager, CSI Driver, ALB/HTTPS e GitHub Actions com AWS OIDC.

---

## 🏢 Multi-Tenancy e RLS

```text
JWT tenant_id
    ↓
TenantContext
    ↓
SET LOCAL ROLE tenantguard_app
    ↓
app.current_tenant
    ↓
PostgreSQL RLS
    ↓
Dados do tenant autenticado
```

`ROLE_ADMIN` concede funções administrativas, não acesso global aos dados de outros tenants. Recursos cross-tenant são tratados como não encontrados.

---

## 🧪 Testes

```bash
mvn test
```

Baseline local validado em 07/09/2026:

```text
Tests run: 90
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A suíte cobre domínio, policy, fingerprint, deduplicação, RBAC, API administrativa, auditoria, métricas, RLS e isolamento cross-tenant com PostgreSQL via Testcontainers.

O Sprint 15 somente será promovido como integrado à `main` após CI do head final, validação runtime e revisão/merge do PR.

---

## 🐳 Execução local

```bash
git clone https://github.com/juceliocoelho2022/tenantguard-java.git
cd tenantguard-java
cp .env.example .env
# configure apenas valores locais seguros
docker compose up -d --build
```

Ou via Maven, com PostgreSQL/Redis e variáveis de ambiente configuradas:

```bash
mvn spring-boot:run
```

> Nunca faça commit de `.env`, API keys, JWT secrets, senhas ou tokens reais.

---

## 📊 Observabilidade local

```text
API        http://localhost:8081
Swagger    http://localhost:8081/swagger-ui/index.html
Health     http://localhost:8081/actuator/health
Readiness  http://localhost:8081/actuator/health/readiness
Liveness   http://localhost:8081/actuator/health/liveness
Prometheus http://localhost:9090
Grafana    http://localhost:3000
```

---

## ☸️ Kubernetes e AWS

O repositório contém manifests Kubernetes e Terraform para EKS, RDS PostgreSQL, ElastiCache Redis, ECR, ALB, Secrets Manager, Secrets Store CSI Driver e GitHub Actions com AWS OIDC.

---

## 🔒 Invariantes de segurança

- tenant derivado do JWT validado;
- RBAC e isolamento multi-tenant são controles independentes;
- consultas tenant-aware + PostgreSQL RLS fornecem defesa em profundidade;
- refresh tokens possuem rotação/revogação e replay detection;
- Redis fornece rate limiting distribuído;
- IA não controla score, risk level ou decisões críticas;
- fallback determinístico mantém disponibilidade sem provedor de IA;
- incident policy é determinística e testável;
- fingerprint não depende de saída da IA;
- incident lifecycle é auditado;
- métricas evitam tags de alta cardinalidade;
- secrets de runtime são externalizados;
- containers executam como non-root;
- AWS OIDC reduz dependência de credenciais estáticas.

---

## 📁 Estrutura conceitual

```text
src/main/java/com/jucelio/tenantguard/
  ├── auth/
  ├── security/
  ├── securityintelligence/
  ├── securityincident/
  ├── audit/
  ├── order/
  ├── tenant/
  └── observability/

src/main/resources/db/migration/   Flyway
src/test/                          testes automatizados
observability/                     Prometheus, Grafana, Loki, Tempo
k8s/                               Kubernetes
infra/terraform/                   AWS como código
.github/workflows/                 CI/CD
```

---

## 🗺️ Roadmap

- [x] JWT + RBAC + TenantContext
- [x] PostgreSQL RLS
- [x] refresh token rotation/revocation
- [x] replay detection
- [x] Redis distributed rate limiting
- [x] security event audit
- [x] observabilidade
- [x] Docker non-root
- [x] Kubernetes + Terraform AWS
- [x] GitHub Actions + AWS OIDC
- [x] Security Intelligence determinística
- [x] Spring AI enrichment com fallback seguro
- [x] métricas de AI security intelligence
- [x] Security Incident domain/lifecycle
- [x] persistência JPA + optimistic locking
- [x] RLS para security incidents
- [x] deduplicação ativa + fingerprint estável
- [x] API administrativa tenant-aware
- [x] auditoria de incident lifecycle
- [x] métricas de incident response
- [x] testes cross-tenant/Testcontainers
- [ ] CI do head final do Sprint 15
- [ ] validação runtime final do Sprint 15
- [ ] PR Sprint 15 revisado e integrado à `main`
- [ ] ambiente AWS/EKS demonstrativo end-to-end
- [ ] testes de carga e SLO/SLI formais

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**  
Java Backend Developer

`Java` • `Spring Boot` • `Spring AI` • `REST APIs` • `PostgreSQL` • `Redis` • `Docker` • `Kubernetes` • `Terraform` • `AWS` • `Spring Security` • `JWT` • `RBAC` • `Testcontainers` • `Multi-Tenant Architecture` • `RLS` • `Prometheus` • `Grafana` • `OpenTelemetry`

---

## 📄 Aviso

Este repositório é uma PoC educacional e de portfólio. Credenciais e secrets reais não devem ser versionados.