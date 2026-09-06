# 🛡️ TenantGuard Java

<p align="center">
  <a href="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml">
    <img src="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml/badge.svg?branch=main" alt="TenantGuard CI">
  </a>
  <img src="https://img.shields.io/badge/Release-v1.0.0-blue" alt="v1.0.0">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen" alt="Spring Boot 3.5">
  <img src="https://img.shields.io/badge/PostgreSQL-RLS-blue" alt="PostgreSQL RLS">
  <img src="https://img.shields.io/badge/Security-RBAC-success" alt="RBAC">
  <img src="https://img.shields.io/badge/Tests-40%20passing-success" alt="40 tests passing">
</p>

<p align="center">
  <img src="docs/images/tenantGuard-architecture.png" alt="TenantGuard Java - Secure Multi-Tenant Architecture" width="100%">
</p>

**TenantGuard Java** é uma Proof of Concept de backend SaaS multi-tenant construída com **Java 21 e Spring Boot**, evoluída até um baseline `v1.0.0` com segurança em profundidade, observabilidade, resiliência e infraestrutura como código para AWS/Kubernetes.

O tenant é derivado do JWT autenticado, propagado pelo `TenantContext`, aplicado nas consultas e reforçado pelo PostgreSQL através de **Row Level Security (RLS)**.

> O projeto é destinado a estudo, ensino, portfólio e demonstração arquitetural. A presença de controles de produção não significa que a PoC seja um SaaS comercial pronto para uso sem revisão operacional, de segurança, custos, compliance e SRE.

---

## 🎯 Objetivo

Demonstrar como construir um backend SaaS com **Shared Database + Shared Schema + `tenant_id`**, evitando que o cliente escolha o tenant e aplicando controles complementares em autenticação, autorização, persistência, auditoria e infraestrutura.

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
  ├──► Security/Audit Events
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

## 🚀 Stack

### Backend e segurança

- Java 21
- Spring Boot 3.5
- Spring Security
- Access Token + Refresh Token JWT
- rotação e revogação de refresh tokens
- RBAC — `USER` / `ADMIN`
- Tenant Resolver / Tenant Context
- Spring Data JPA / Hibernate
- PostgreSQL 17 + Row Level Security
- Flyway
- Redis para rate limiting distribuído
- auditoria de operações e eventos de segurança
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

Exemplo conceitual:

| id | description | tenant_id |
|---:|---|---|
| 1 | Pedido A-001 | TENANT_A |
| 2 | Pedido A-002 | TENANT_A |
| 3 | Pedido B-001 | TENANT_B |
| 4 | Pedido C-001 | TENANT_C |
| 5 | Pedido C-002 | TENANT_C |

O PostgreSQL reforça o isolamento com RLS e `WITH CHECK`, protegendo leitura e escrita. As roles de runtime e auditoria são explicitamente controladas, e as tabelas de refresh sessions e security events também fazem parte da estratégia de isolamento.

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
TENANT_A → GET /api/orders/3 → 404 Not Found
```

---

## 🔐 Autenticação, autorização e sessões

O fluxo de autenticação utiliza access e refresh tokens com separação de finalidade. Refresh tokens possuem sessão persistida, rotação e revogação; um token antigo não pode ser reutilizado após a rotação.

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

RBAC validado:

```text
sem autenticação → endpoint protegido → 401
USER → /api/admin/status → 403
ADMIN → /api/admin/status → 200
```

`ROLE_ADMIN` não concede acesso global aos dados de outros tenants.

---

## 🚦 Rate Limiting distribuído

O projeto utiliza Redis como backend de rate limiting. Em validação runtime com tentativas inválidas de login, as primeiras tentativas foram tratadas como falha de autenticação e, após o limite configurado, novas tentativas passaram a retornar `429 Too Many Requests`.

Isso evita que o controle dependa exclusivamente da memória de uma única instância da aplicação.

---

## 🔎 Auditoria e rastreabilidade

Operações de domínio e eventos de segurança são correlacionados com contexto técnico da requisição. Os logs estruturados incluem, conforme o fluxo:

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

A correlação foi validada em runtime também para respostas de segurança `401` e `403`, permitindo relacionar requisição, usuário/tenant quando disponível, resultado HTTP e tracing.

---

## 📊 Observabilidade

A stack local inclui:

| Componente | Finalidade |
|---|---|
| Actuator / Micrometer | health e métricas da aplicação |
| Prometheus | coleta de métricas |
| Grafana | dashboards e exploração |
| JSON Logs | logs estruturados |
| Promtail / Loki | coleta e consulta de logs |
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

## 🐳 Docker

O runtime Docker executa com usuário não-root (`UID/GID 10001`). O diretório de logs recebe ownership explícito durante o build para permitir logging sem remover o hardening do container.

```bash
git clone https://github.com/juceliocoelho2022/tenantguard-java.git
cd tenantguard-java
cp .env.example .env
# configure os valores locais necessários no .env
docker compose up -d --build
```

Validação realizada no baseline v1:

```text
/actuator/health           → UP
/actuator/health/liveness  → UP
/actuator/health/readiness → UP
```

> Nunca faça commit do arquivo `.env` nem reutilize secrets demonstrativos em ambientes reais.

---

## ☸️ Kubernetes e AWS

O repositório contém manifests Kubernetes e infraestrutura Terraform para uma arquitetura AWS com EKS, RDS PostgreSQL, ElastiCache Redis, ECR, ALB, Secrets Manager e integração de workload identity.

A entrega usa GitHub Actions com OIDC para evitar credenciais AWS estáticas no pipeline. O JWT signing secret é tratado fora do Terraform state e disponibilizado ao workload por Secrets Manager/CSI.

O Terraform foi inicializado localmente com backend desabilitado para validação e `terraform validate` retornou configuração válida.

> A validação server-side dos manifests Kubernetes contra EKS permanece dependente de um cluster real. Na verificação do baseline v1 não havia cluster EKS provisionado na região AWS utilizada, portanto essa etapa não é apresentada como validada em runtime.

---

## 🧪 Testes automatizados e CI

A suíte usa testes unitários/de integração, MockMvc e PostgreSQL real via Testcontainers.

```bash
mvn clean test
```

Validação final antes do marco `v1.0.0`:

```text
Tests run: 40
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O **TenantGuard CI** também concluiu com sucesso no PR de hardening final do container.

### Matriz de segurança validada em runtime

| Cenário | Resultado |
|---|---:|
| Endpoint protegido sem autenticação | `401` |
| USER em endpoint ADMIN | `403` |
| ADMIN em endpoint ADMIN | `200` |
| Recurso de outro tenant | `404` |
| Rate limit excedido | `429` |
| Replay de refresh token após rotação | `401` |
| Correlation/trace IDs em eventos 401/403 | Validado |
| Health / liveness / readiness | `UP` |

---

## 🔒 Decisões de segurança

- tenant derivado do JWT validado, nunca escolhido pelo cliente;
- RBAC e isolamento multi-tenant são controles independentes e complementares;
- recursos cross-tenant retornam `404` para reduzir exposição de existência;
- consultas tenant-aware + PostgreSQL RLS formam defesa em profundidade;
- RLS também protege dados sensíveis de autenticação/auditoria;
- refresh tokens possuem rotação/revogação e proteção contra replay;
- Redis fornece rate limiting distribuído;
- secrets de runtime são externalizados;
- JWT signing key não é persistida no Terraform state;
- containers executam como non-root;
- health/readiness/liveness permitem integração com orquestração;
- AWS OIDC reduz necessidade de credenciais estáticas no CI/CD;
- logs e eventos carregam identificadores de correlação para investigação.

---

## 📁 Estrutura conceitual

```text
src/                  aplicação e testes
observability/        Prometheus, Grafana, Loki, Promtail, Tempo
k8s/                  manifests Kubernetes
infra/terraform/      infraestrutura AWS como código
.github/workflows/    CI, entrega e bootstrap de runtime secrets
Dockerfile            imagem non-root
docker-compose.yml    stack local
```

---

## ✅ Evolução concluída até o baseline v1

- [x] JWT com tenant e RBAC
- [x] Access + Refresh Token
- [x] rotação/revogação e proteção contra replay
- [x] TenantContext
- [x] consultas tenant-aware
- [x] PostgreSQL RLS
- [x] RLS para dados de autenticação e segurança
- [x] auditoria persistente por tenant
- [x] security event audit
- [x] Redis distributed rate limiting
- [x] OpenAPI / Swagger
- [x] Actuator / Micrometer / Prometheus / Grafana
- [x] JSON Logs / Loki / Promtail
- [x] OpenTelemetry / Tempo
- [x] correlation ID / trace ID / span ID
- [x] Docker Compose
- [x] container non-root
- [x] health / liveness / readiness
- [x] Kubernetes + HPA
- [x] Terraform AWS
- [x] EKS / RDS / ElastiCache / ECR / ALB
- [x] Secrets Manager + CSI
- [x] GitHub Actions CI
- [x] GitHub OIDC para AWS
- [x] remote Terraform state
- [x] 40 testes verdes no baseline v1

### Próximas evoluções possíveis

- [ ] provisionar ambiente AWS/EKS de demonstração e executar validação end-to-end
- [ ] adicionar testes de carga e metas formais de SLO/SLI
- [ ] adicionar alertas operacionais e política de retenção
- [ ] integrar um IdP/OIDC corporativo, como Keycloak, quando fizer sentido para o caso de uso
- [ ] avaliar arquitetura orientada a eventos para fluxos que realmente exijam desacoplamento assíncrono

---

## 🏷️ Release

Baseline estável marcado como **`v1.0.0`**.

A tag representa o código validado antes desta atualização documental; a documentação posterior preserva a imutabilidade do marco de release.

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**  
Java Backend Developer

`Java` • `Spring Boot` • `APIs REST` • `PostgreSQL` • `Redis` • `Docker` • `Kubernetes` • `Terraform` • `AWS` • `Spring Security` • `JWT` • `RBAC` • `Testcontainers` • `Multi-Tenant Architecture` • `RLS` • `Prometheus` • `Grafana` • `OpenTelemetry`

---

## 📄 Aviso

Credenciais, usuários e configurações demonstrativas destinam-se exclusivamente a desenvolvimento local e estudo. Não reutilize senhas, tokens ou secrets de demonstração em ambientes reais.
