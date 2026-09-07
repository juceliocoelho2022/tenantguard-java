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
  <img src="https://img.shields.io/badge/Tests-59%20baseline-success" alt="59 tests passing on Sprint 14 baseline">
</p>

<p align="center">
  <img src="docs/images/tenantGuard-architecture.png" alt="TenantGuard Java - Secure Multi-Tenant Architecture" width="100%">
</p>

**TenantGuard Java** é uma Proof of Concept de backend SaaS multi-tenant construída com **Java 21 e Spring Boot**, evoluída com segurança em profundidade, isolamento por tenant, observabilidade, resiliência, infraestrutura como código e inteligência de segurança assistida por IA.

O tenant é derivado do JWT autenticado, propagado pelo `TenantContext`, aplicado nas consultas e reforçado pelo PostgreSQL através de **Row Level Security (RLS)**. A IA nunca substitui as decisões determinísticas de segurança: `riskScore`, `riskLevel`, categorias e políticas críticas continuam sob controle do backend.

> Projeto voltado a estudo, ensino, portfólio e demonstração arquitetural. A presença de controles de produção não significa que a PoC seja um SaaS comercial pronto para uso sem revisão operacional, segurança, custos, compliance e SRE.

---

## 🚀 Estado atual

| Marco | Status |
|---|---|
| Baseline v1 — multi-tenancy, RLS, JWT, observabilidade e AWS IaC | ✅ concluído |
| Sprint 13 — Security Intelligence determinística + token replay | ✅ concluído |
| Sprint 14 — Spring AI + fallback seguro + métricas | ✅ concluído e integrado à `main` |
| Sprint 15 — Security Incident Response | 🚧 em desenvolvimento |

Validação mais recente integrada à `main`:

```text
Tests run: 59
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O Sprint 15 adiciona novos testes e componentes; a contagem acima permanece como o último baseline totalmente validado até a próxima execução da suíte.

---

## 🎯 Objetivo

Demonstrar como construir um backend SaaS com **Shared Database + Shared Schema + `tenant_id`**, evitando que o cliente escolha o tenant e aplicando controles complementares em autenticação, autorização, persistência, auditoria, detecção de risco, resposta a incidentes e infraestrutura.

```text
Cliente
  │
  ▼
Spring Security + Access JWT
  │   ├── tenant_id
  │   ├── user
  │   └── role
  ▼
TenantContext + RBAC
  │
  ├──► Service / Repository
  │        │
  │        ▼
  │    PostgreSQL + RLS
  │
  ├──► Security / Audit Events
  │        │
  │        ▼
  │    Security Intelligence
  │        │
  │        ├──► Deterministic Risk Engine
  │        └──► Spring AI enrichment
  │                  │
  │                  ▼
  │            Safe Fallback
  │
  ├──► Security Incident Policy
  │        │
  │        ▼
  │    Incident Lifecycle
  │
  ├──► Redis Rate Limiting
  │
  └──► Observability
          ├── Prometheus
          ├── Grafana
          ├── Loki / Promtail
          └── OpenTelemetry / Tempo
```

---

## 🧠 Security Intelligence + Spring AI

O módulo de Security Intelligence analisa eventos de segurança do tenant e produz uma avaliação determinística com:

- `riskScore`;
- `riskLevel`;
- categorias de sinal;
- findings;
- recomendações.

Categorias atuais incluem:

```text
AUTH_FAILURE
ACCESS_DENIED
RATE_LIMIT
TOKEN_REPLAY
GENERIC_FAILURE
```

O Spring AI pode enriquecer **findings** e **recommendations**, mas não pode alterar o score ou a classificação determinística.

### Fail-safe / graceful degradation

Se o provedor de IA estiver indisponível, retornar `null`, atingir timeout ou responder com erro, o TenantGuard continua operacional usando a análise determinística.

Também existe short-circuit para evidência vazia:

```text
0 security events
      ↓
deterministic result
      ↓
AI provider is not called
```

Isso evita chamadas externas sem valor, consumo de quota, latência e ruído de fallback.

Métricas Micrometer do fluxo de IA:

```text
tenantguard.security.intelligence.ai.attempts
tenantguard.security.intelligence.ai.successes
tenantguard.security.intelligence.ai.failures
tenantguard.security.intelligence.ai.fallbacks
tenantguard.security.intelligence.ai.latency
```

---

## 🚨 Sprint 15 — Security Incident Response

O próximo estágio do TenantGuard transforma detecção em resposta operacional.

```text
Security Events
      ↓
Security Intelligence
      ↓
Deterministic Incident Policy
      ↓
Security Incident
      ↓
OPEN → INVESTIGATING → RESOLVED
  └────────────────────→ DISMISSED
```

O núcleo inicial do Sprint 15 já define:

- `SecurityIncident`;
- `SecurityIncidentStatus`;
- `SecurityIncidentSeverity`;
- `SecurityIncidentDecision`;
- `SecurityIncidentPolicy`;
- lifecycle explícito;
- policy determinística para abertura de incidentes;
- fingerprint SHA-256 para futura deduplicação;
- testes unitários de decisão e transição de estado.

A policy inicial abre incidente quando o score atinge o threshold definido ou quando um sinal crítico, como `TOKEN_REPLAY`, exige tratamento independente do score.

### Princípio arquitetural

A decisão de abrir, classificar, resolver ou descartar incidentes **não é delegada ao LLM**. A IA pode enriquecer contexto narrativo, mas regras de segurança permanecem determinísticas e testáveis.

Próximas entregas do Sprint 15:

- persistência JPA dos incidentes;
- Flyway migration;
- PostgreSQL RLS para incidentes;
- deduplicação por fingerprint;
- optimistic locking;
- API administrativa tenant-aware;
- auditoria de transições;
- métricas de incident response;
- testes cross-tenant e integração.

---

## 🚀 Stack

### Backend e segurança

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring AI 1.1.8
- Access Token + Refresh Token JWT
- rotação e revogação de refresh tokens
- RBAC — `USER` / `ADMIN`
- Tenant Resolver / Tenant Context
- Spring Data JPA / Hibernate
- PostgreSQL 17 + Row Level Security
- Flyway
- Redis para rate limiting distribuído
- auditoria e security events
- correlação por `requestId`, `correlationId`, `traceId` e `spanId`

### Qualidade e observabilidade

- JUnit 5
- MockMvc
- Testcontainers
- GitHub Actions CI
- Spring Boot Actuator
- Micrometer / Prometheus
- Grafana
- logs estruturados JSON
- Loki / Promtail
- OpenTelemetry / Tempo
- health, liveness e readiness probes

### Infraestrutura e entrega

- Docker / Docker Compose
- container non-root
- Kubernetes
- Horizontal Pod Autoscaler
- Terraform
- AWS EKS
- Amazon RDS for PostgreSQL
- Amazon ElastiCache for Redis
- Amazon ECR
- AWS Secrets Manager
- Secrets Store CSI Driver
- Application Load Balancer / HTTPS
- GitHub Actions + AWS OIDC
- Terraform remote state em S3

---

## 🏢 Multi-Tenancy e RLS

O `tenant_id` não é aceito do cliente por query string, path parameter ou body. Ele é obtido do JWT validado.

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
Somente dados do tenant autenticado
```

Acesso cross-tenant validado em runtime:

```text
TENANT_A → recurso pertencente ao TENANT_B → 404 Not Found
```

`ROLE_ADMIN` não concede acesso global aos dados de outros tenants.

---

## 🔐 Autenticação, autorização e sessões

```text
Login
  ↓
Access Token + Refresh Token
  │
  ├── Access Token → APIs protegidas
  │
  └── Refresh Token → /api/auth/refresh
                         ↓
                    rotação/revogação
```

Matriz validada:

| Cenário | Resultado |
|---|---:|
| Endpoint protegido sem autenticação | `401` |
| USER em endpoint ADMIN | `403` |
| ADMIN em endpoint ADMIN | `200` |
| Recurso de outro tenant | `404` |
| Rate limit excedido | `429` |
| Replay de refresh token | `401` |

---

## 🚦 Rate Limiting distribuído

O Redis atua como backend de rate limiting, evitando que o controle dependa da memória de uma única instância. Após o limite configurado, novas requisições podem retornar `429 Too Many Requests`.

---

## 🔎 Auditoria e rastreabilidade

Logs e eventos podem carregar:

```text
tenant_id
user
role
requestId
correlationId
traceId
spanId
status
durationMs
```

Esse contexto permite relacionar autenticação, autorização, eventos de segurança, análise de risco e futuras transições de incident response.

---

## 📊 Observabilidade

| Componente | Finalidade |
|---|---|
| Actuator / Micrometer | health e métricas |
| Prometheus | coleta de métricas |
| Grafana | dashboards |
| JSON Logs | logs estruturados |
| Promtail / Loki | centralização de logs |
| OpenTelemetry / Tempo | tracing distribuído |

Endpoints locais principais:

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

## ☸️ Kubernetes e AWS

O repositório contém manifests Kubernetes e infraestrutura Terraform para arquitetura com:

- EKS;
- RDS PostgreSQL;
- ElastiCache Redis;
- ECR;
- ALB;
- Secrets Manager;
- Secrets Store CSI Driver;
- GitHub Actions com AWS OIDC.

O JWT signing secret é tratado fora do Terraform state e disponibilizado ao workload por mecanismo de secrets externo.

---

## 🧪 Testes e CI

```bash
mvn test
```

Baseline do Sprint 14 validado localmente e em CI:

```text
Tests run: 59
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O Sprint 15 acrescenta novos testes; a nova contagem será promovida no README somente após execução completa da suíte.

---

## 🔒 Decisões de segurança

- tenant derivado do JWT validado;
- RBAC e isolamento multi-tenant como controles independentes;
- recursos cross-tenant retornam `404`;
- consultas tenant-aware + PostgreSQL RLS formam defesa em profundidade;
- refresh tokens possuem rotação/revogação e proteção contra replay;
- Redis fornece rate limiting distribuído;
- IA não controla score, risk level ou decisões críticas;
- fallback determinístico mantém disponibilidade sem provedor de IA;
- evidência enviada ao LLM é limitada e passa por redaction;
- incident policy é determinística e testável;
- fingerprint prepara deduplicação de incidentes;
- secrets de runtime são externalizados;
- containers executam como non-root;
- AWS OIDC reduz dependência de credenciais estáticas;
- logs e eventos carregam identificadores de correlação.

---

## 📁 Estrutura conceitual

```text
src/main/java/
  └── com/jucelio/tenantguard/
      ├── auth/
      ├── security/
      ├── securityintelligence/
      ├── securityincident/        # Sprint 15
      ├── audit/
      ├── order/
      ├── tenant/
      └── observability/

src/test/                    testes automatizados
observability/               Prometheus, Grafana, Loki, Tempo
k8s/                         manifests Kubernetes
infra/terraform/             infraestrutura AWS como código
.github/workflows/           CI/CD
Dockerfile                   imagem non-root
docker-compose.yml           stack local
```

---

## 🗺️ Roadmap

- [x] JWT + RBAC + TenantContext
- [x] PostgreSQL RLS
- [x] refresh token rotation/revocation
- [x] replay detection
- [x] Redis distributed rate limiting
- [x] security event audit
- [x] observabilidade completa
- [x] Docker non-root
- [x] Kubernetes + Terraform AWS
- [x] GitHub Actions + AWS OIDC
- [x] Security Intelligence determinística
- [x] Spring AI enrichment com fallback seguro
- [x] métricas de AI security intelligence
- [x] short-circuit para evidência vazia
- [x] núcleo de domínio do Security Incident Response
- [ ] persistência de security incidents
- [ ] RLS para security incidents
- [ ] deduplicação por fingerprint
- [ ] optimistic locking
- [ ] API administrativa de incidentes
- [ ] métricas e auditoria de incident lifecycle
- [ ] testes cross-tenant de incident response
- [ ] ambiente AWS/EKS demonstrativo end-to-end
- [ ] testes de carga e SLO/SLI formais

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**  
Java Backend Developer

`Java` • `Spring Boot` • `Spring AI` • `REST APIs` • `PostgreSQL` • `Redis` • `Docker` • `Kubernetes` • `Terraform` • `AWS` • `Spring Security` • `JWT` • `RBAC` • `Testcontainers` • `Multi-Tenant Architecture` • `RLS` • `Prometheus` • `Grafana` • `OpenTelemetry`

---

## 📄 Aviso

Credenciais, usuários e configurações demonstrativas destinam-se exclusivamente a desenvolvimento local e estudo. Não reutilize senhas, tokens, API keys ou secrets de demonstração em ambientes reais.
