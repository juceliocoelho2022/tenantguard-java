# 🛡️ TenantGuard Java

<p align="center">
  <a href="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml">
    <img src="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml/badge.svg?branch=main" alt="TenantGuard CI">
  </a>
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen" alt="Spring Boot 3.5">
  <img src="https://img.shields.io/badge/PostgreSQL-RLS-blue" alt="PostgreSQL RLS">
  <img src="https://img.shields.io/badge/Security-RBAC-success" alt="RBAC">
  <img src="https://img.shields.io/badge/OpenAPI-Swagger-85EA2D" alt="OpenAPI Swagger">
  <img src="https://img.shields.io/badge/Observability-Prometheus%20%7C%20Grafana-orange" alt="Observability">
  <img src="https://img.shields.io/badge/Tracing-OpenTelemetry-purple" alt="OpenTelemetry">
</p>

<p align="center">
  <img src="docs/images/tenantGuard-architecture.png"
       alt="TenantGuard Java - Secure Multi-Tenant Architecture"
       width="100%">
</p>

**TenantGuard Java** é uma Proof of Concept de backend SaaS Multi-Tenant construída com **Java 21 e Spring Boot**, combinando isolamento de dados, autenticação JWT, RBAC, PostgreSQL Row Level Security, auditoria persistente por tenant e uma stack completa de observabilidade.

O projeto demonstra defesa em profundidade: o tenant é derivado do JWT autenticado, aplicado na camada de serviço/repositório e reforçado pelo próprio PostgreSQL através de RLS.

---

## 🎯 Objetivo

A arquitetura utiliza:

**Shared Database + Shared Schema + `tenant_id` + PostgreSQL RLS**

O `tenant_id` **não é aceito do cliente** por query string, path parameter ou body. Ele é obtido do JWT validado e propagado pelo `TenantContext`.

Isso reduz o risco de acesso horizontal indevido e mantém o isolamento mesmo quando diferentes clientes compartilham o mesmo banco e as mesmas tabelas.

---

## 🏗️ Arquitetura

```text
Cliente
  │
  ▼
Spring Security + JWT
  │
  ├── tenant_id
  ├── user
  └── role
  │
  ▼
TenantContext
  │
  ├──────────────► PostgreSQL 17
  │                  ├── tenant-aware queries
  │                  ├── SET LOCAL ROLE tenantguard_app
  │                  ├── app.current_tenant
  │                  ├── Row Level Security
  │                  └── Audit Events + RLS
  │
  ├──────────────► Micrometer / Actuator
  │                    │
  │                    ▼
  │                Prometheus
  │
  ├──────────────► JSON Logs ──► Promtail ──► Loki
  │
  └──────────────► OpenTelemetry ───────────► Tempo
                                               │
                                               ▼
                                            Grafana
```

### Defesa em profundidade

```text
JWT tenant_id + role
     │
     ├──► Spring Security RBAC
     │
     ▼
TenantContext
     │
     ├──► Repository: id + tenant_id
     │
     ▼
PostgreSQL session: app.current_tenant
     │
     ▼
RLS Policy
     │
     ▼
Somente linhas do tenant autenticado
```

---

## 🚀 Stack

### Backend e segurança

- Java 21
- Spring Boot 3.5
- Spring Security
- JWT
- RBAC — Role-Based Access Control
- Spring Data JPA / Hibernate
- Tenant Resolver / Tenant Context
- PostgreSQL Row Level Security
- Flyway
- Maven

### Observabilidade

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Logs estruturados JSON
- Loki
- Promtail
- OpenTelemetry
- Grafana Tempo
- Correlação por `requestId`, `traceId`, `spanId` e `tenant_id`

### Qualidade e infraestrutura

- Docker / Docker Compose
- OpenAPI / Swagger
- JUnit 5
- MockMvc
- Testcontainers
- PostgreSQL Testcontainer
- GitHub Actions CI

---

## 🏢 Estratégia Multi-Tenant

Exemplo conceitual da tabela `orders`:

| id | description | tenant_id |
|---:|---|---|
| 1 | Pedido A-001 | TENANT_A |
| 2 | Pedido A-002 | TENANT_A |
| 3 | Pedido B-001 | TENANT_B |
| 4 | Pedido C-001 | TENANT_C |
| 5 | Pedido C-002 | TENANT_C |

Mesmo compartilhando a tabela, cada tenant visualiza somente os próprios registros.

### PostgreSQL Row Level Security

A aplicação habilita e força RLS na tabela `orders` e utiliza uma policy baseada no tenant configurado na sessão PostgreSQL.

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;

CREATE POLICY orders_tenant_isolation
ON orders
USING (tenant_id = current_setting('app.current_tenant', true))
WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
```

Antes das operações protegidas:

```text
JWT
 ↓
TenantContext
 ↓
SET LOCAL ROLE tenantguard_app
 ↓
set_config('app.current_tenant', tenantId, true)
 ↓
PostgreSQL RLS
```

O `WITH CHECK` impede que a role da aplicação grave registros para outro tenant.

---

## 👥 RBAC — Role-Based Access Control

O claim `role` é convertido em authority do Spring Security:

```text
USER  → ROLE_USER
ADMIN → ROLE_ADMIN
```

Endpoints administrativos usam `hasRole("ADMIN")`.

Validações realizadas:

```text
USER    → /api/admin/** → 403 Forbidden
ADMIN   → /api/admin/** → 200 OK
sem JWT → endpoint protegido → 401 Unauthorized
```

Possuir `ROLE_ADMIN` **não concede acesso global**. O administrador continua restrito ao tenant presente no JWT.

---

## 🔐 Usuários de demonstração

| Usuário | Senha | Tenant | Role |
|---|---|---|---|
| `user-a` | `password` | `TENANT_A` | `USER` |
| `user-b` | `password` | `TENANT_B` | `USER` |
| `user-c` | `password` | `TENANT_C` | `USER` |
| `admin-a` | `password` | `TENANT_A` | `ADMIN` |

> As credenciais acima existem exclusivamente para demonstração e ambiente local.

---

## 🔎 Auditoria persistente por tenant

A migration `V3__create_audit_events.sql` cria `audit_events` com RLS e índices para tenant, data e trace.

Eventos atualmente registrados:

```text
ORDER_LIST
ORDER_READ
ORDER_CREATE
```

Cada evento armazena:

| Campo | Finalidade |
|---|---|
| `tenantId` | Tenant responsável pela operação |
| `username` | Usuário autenticado |
| `action` | Ação executada |
| `resourceType` | Tipo do recurso |
| `resourceId` | Identificador do recurso, quando aplicável |
| `outcome` | Resultado da ação |
| `requestId` | Correlação da requisição HTTP |
| `traceId` | Correlação com tracing distribuído |
| `createdAt` | Data/hora UTC |

Endpoint administrativo:

```http
GET /api/admin/audit-events
Authorization: Bearer <ADMIN_JWT>
```

Validação funcional realizada:

```text
user-b  / TENANT_B / USER  → 403
admin-a / TENANT_A / ADMIN → 200
```

O `admin-a` recebeu somente eventos do `TENANT_A`, mesmo após tráfego produzido pelo `TENANT_B`, comprovando que RBAC e isolamento de tenant permanecem complementares.

Exemplo:

```json
{
  "tenantId": "TENANT_A",
  "username": "user-a",
  "action": "ORDER_READ",
  "resourceType": "ORDER",
  "resourceId": "1",
  "outcome": "SUCCESS",
  "requestId": "<request-id>",
  "traceId": "<trace-id>"
}
```

---

## 📊 Sprint 4 — Observabilidade Enterprise ✅

A Sprint 4 evoluiu o TenantGuard de uma PoC focada em segurança Multi-Tenant para um case de backend com métricas, logs estruturados, tracing e auditoria.

| Etapa | Status | Resultado |
|---:|:---:|---|
| 1 | ✅ | Micrometer + Actuator |
| 2 | ✅ | Prometheus |
| 3 | ✅ | Grafana Dashboard |
| 4 | ✅ | Logs JSON + Loki + Promtail + correlação por tenant |
| 5 | ✅ | OpenTelemetry + Tempo + `traceId` / `spanId` nos logs |
| 6 | ✅ | Auditoria persistente por tenant protegida por RLS |

### Prometheus

A aplicação expõe métricas Micrometer através do Actuator, coletadas pelo Prometheus.

```text
http://localhost:8081/actuator/prometheus
http://localhost:9090
```

### Grafana

Dashboard provisionado para acompanhar:

- HTTP Throughput
- Average HTTP Latency
- JVM Memory Used
- Application CPU Usage
- JVM Live Threads
- HikariCP Connections

```text
http://localhost:3000
```

### Logs estruturados + Loki

Os logs JSON incluem dados de correlação como:

```text
tenant_id
user
role
requestId
traceId
spanId
status
durationMs
```

O Promtail coleta os arquivos estruturados e os envia ao Loki para consulta no Grafana Explore.

### OpenTelemetry + Tempo

O projeto utiliza Micrometer Tracing com bridge OpenTelemetry e exportação OTLP para Tempo. A correlação `traceId`/`spanId` foi validada nos logs estruturados, permitindo relacionar requisições, logs e auditoria.

> O sampling está configurado em `1.0` para a PoC. Em produção deve ser ajustado conforme volume, custo e política de observabilidade.

---

## 🐳 Executando com Docker

### Pré-requisitos

- Docker Desktop
- Docker Compose

```bash
git clone https://github.com/juceliocoelho2022/tenantguard-java.git
cd tenantguard-java
docker compose up --build -d
```

Serviços locais:

| Serviço | URL / Porta |
|---|---|
| TenantGuard API | `http://localhost:8081` |
| Swagger UI | `http://localhost:8081/swagger-ui/index.html` |
| Grafana | `http://localhost:3000` |
| Prometheus | `http://localhost:9090` |
| Loki | `http://localhost:3100` |
| Tempo | `http://localhost:3200` |
| PostgreSQL | `localhost:5432` |

Para acompanhar containers:

```bash
docker compose ps
docker compose logs app --tail=100
```

---

## 🔑 Autenticação e Swagger

Login:

```http
POST /api/auth/login
```

```json
{
  "username": "user-a",
  "password": "password"
}
```

Depois copie o JWT retornado, abra o Swagger e use **Authorize**.

```text
http://localhost:8081/swagger-ui/index.html
```

---

## 🛡️ Testes de isolamento

O pedido `3` pertence ao `TENANT_B`:

```text
TENANT_A ──► GET /api/orders/3 ──► 404 Not Found
TENANT_B ──► GET /api/orders/3 ──► 200 OK
```

Os testes também comprovam que o PostgreSQL RLS:

- filtra linhas por tenant mesmo sem predicate explícito de `tenant_id` no SQL;
- bloqueia inserções cross-tenant através do `WITH CHECK`;
- mantém administradores limitados ao tenant autenticado;
- protege a leitura dos registros de auditoria por tenant.

---

## 🧪 Testes automatizados e CI

A suíte utiliza PostgreSQL real via Testcontainers.

```bash
mvn clean test
```

Última validação local da Sprint 4:

```text
Tests run: 11
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A cobertura de integração inclui isolamento multi-tenant, RLS, RBAC, respostas explícitas `401/403` e auditoria por tenant.

O workflow **TenantGuard CI** executa automaticamente build e testes em `push` e `pull request` para `main`.

---

## 🔒 Decisões de segurança

- O cliente não escolhe o `tenantId`; ele vem do JWT validado.
- `ROLE_ADMIN` não ignora isolamento de tenant.
- Recursos pertencentes a outro tenant retornam `404` para não revelar sua existência.
- Consultas tenant-aware e PostgreSQL RLS são usados juntos como defesa em profundidade.
- `TenantContext` é limpo ao final da requisição.
- A role PostgreSQL `tenantguard_app` é utilizada para executar operações sujeitas a RLS.
- A tabela de auditoria possui sua própria policy de RLS.
- Respostas de autenticação/autorização utilizam handlers REST explícitos para `401` e `403`.

---

## 📁 Estrutura conceitual

```text
src
├── main
│   ├── java/com/jucelio/tenantguard
│   │   ├── admin
│   │   ├── audit
│   │   ├── auth
│   │   ├── config
│   │   ├── order
│   │   ├── security
│   │   └── tenant
│   └── resources
│       └── db/migration
└── test/java/com/jucelio/tenantguard

observability
├── grafana
├── loki
├── prometheus
├── promtail
└── tempo

.github
└── workflows
    └── ci.yml
```

---

## ⚠️ Escopo do projeto

Este projeto é uma **Proof of Concept** criada para estudo, ensino, portfólio e demonstração arquitetural. Ele não deve ser tratado como um SaaS pronto para produção.

Em um ambiente real ainda seriam recomendados, entre outros pontos: secrets management, identidade centralizada, credenciais separadas para migrations e runtime, datasource sem privilégios administrativos, políticas de retenção, alertas, tracing sampling adequado, TLS, rate limiting e hardening da infraestrutura.

---

## 🚀 Roadmap técnico

### Concluído

- [x] JWT com `tenant_id` e `role`
- [x] TenantContext
- [x] Consultas tenant-aware
- [x] Testes de isolamento com Testcontainers
- [x] GitHub Actions CI
- [x] PostgreSQL Row Level Security — RLS
- [x] RBAC — Role-Based Access Control
- [x] OpenAPI / Swagger
- [x] Micrometer + Actuator
- [x] Prometheus
- [x] Grafana Dashboard
- [x] Logs estruturados JSON
- [x] Loki + Promtail
- [x] OpenTelemetry + Tempo
- [x] Auditoria persistente por tenant
- [x] REST handlers explícitos para `401` e `403`

### Próximas evoluções

- [ ] Refresh Token
- [ ] Keycloak
- [ ] OAuth 2.0 / OpenID Connect
- [ ] Rate Limiting
- [ ] Redis com isolamento por tenant
- [ ] Kafka / arquitetura orientada a eventos
- [ ] CD / Deploy automatizado
- [ ] Kubernetes + Helm
- [ ] Deploy AWS
- [ ] Schema-per-Tenant
- [ ] Database-per-Tenant

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**  
Java Backend Developer

`Java` • `Spring Boot` • `APIs REST` • `PostgreSQL` • `Docker` • `Spring Security` • `JWT` • `RBAC` • `OpenAPI` • `Testcontainers` • `Multi-Tenant Architecture` • `RLS` • `Prometheus` • `Grafana` • `Loki` • `OpenTelemetry`

---

## 📄 Aviso

Credenciais, usuários e configurações presentes no projeto destinam-se **exclusivamente a demonstração e desenvolvimento local**. Não reutilize secrets ou senhas demonstrativas em ambientes reais.
