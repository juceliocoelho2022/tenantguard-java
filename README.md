# 🛡️ TenantGuard Java

<p align="center">
  <a href="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml">
    <img src="https://github.com/juceliocoelho2022/tenantguard-java/actions/workflows/ci.yml/badge.svg?branch=main" alt="TenantGuard CI">
  </a>
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen" alt="Spring Boot 3.5">
  <img src="https://img.shields.io/badge/PostgreSQL-RLS-blue" alt="PostgreSQL RLS">
</p>

<p align="center">
  <img src="docs/images/tenantGuard-architecture.png"
       alt="TenantGuard Java - Secure Multi-Tenant Architecture"
       width="100%">
</p>

Projeto demonstrativo de **Multi-Tenancy seguro** desenvolvido com **Java 21 e Spring Boot**, com foco em isolamento de dados entre tenants, autenticação JWT, PostgreSQL Row Level Security (RLS), Docker e testes de integração.

O objetivo é demonstrar, por meio de código e testes automatizados, como construir uma API SaaS em que cada tenant autenticado consegue acessar **somente os próprios dados**, utilizando defesa em profundidade na aplicação e no banco de dados.

---

## 🎯 Objetivo

O TenantGuard Java demonstra uma arquitetura Multi-Tenant utilizando:

**Shared Database + Shared Schema + `tenant_id` + PostgreSQL RLS**

Diferentes tenants compartilham o mesmo banco de dados e as mesmas tabelas. O isolamento é aplicado em duas camadas: consultas tenant-aware na aplicação e políticas de **Row Level Security** no PostgreSQL.

O tenant **não é recebido do cliente** através de query string, path parameter ou body. Ele é identificado a partir do claim `tenant_id` presente no JWT autenticado.

---

## 🏗️ Arquitetura de segurança

```text
Client
  │
  ▼
Spring Security
  │
  ▼
JWT Authentication Filter
  │
  ▼
JwtTenantResolver
  │
  ▼
TenantContext
  │
  ▼
SET LOCAL ROLE tenantguard_app
  │
  ▼
app.current_tenant
  │
  ▼
Service / Repository
  │
  ▼
Hibernate / JPA
  │
  ▼
PostgreSQL Row Level Security
```

### Defesa em profundidade

```text
JWT tenant_id
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

### Backend

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

### Segurança

- JWT
- Spring Security
- Tenant Resolver
- Tenant Context
- Isolamento por `tenant_id`
- PostgreSQL Row Level Security (RLS)
- Role de aplicação sem privilégios de superusuário
- Defesa em profundidade Application + Database

### Banco de dados

- PostgreSQL 17
- Flyway
- Row Level Security

### Infraestrutura e qualidade

- Docker
- Docker Compose
- GitHub Actions CI
- JUnit 5
- MockMvc
- Testcontainers
- PostgreSQL Testcontainer

---

## 🏢 Estratégia Multi-Tenant

```text
Shared Database
      +
Shared Schema
      +
tenant_id
      +
PostgreSQL RLS
```

Exemplo conceitual da tabela de pedidos:

| id | description | tenant_id |
|---:|---|---|
| 1 | Pedido A-001 | TENANT_A |
| 2 | Pedido A-002 | TENANT_A |
| 3 | Pedido B-001 | TENANT_B |
| 4 | Pedido C-001 | TENANT_C |
| 5 | Pedido C-002 | TENANT_C |

Mesmo compartilhando a mesma tabela, cada tenant visualiza somente seus próprios registros.

---

## 🔐 PostgreSQL Row Level Security

A aplicação utiliza **RLS como segunda barreira de isolamento**. A migration habilita e força Row Level Security na tabela `orders` e define uma política baseada no tenant da sessão do PostgreSQL.

Conceitualmente:

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;

CREATE POLICY orders_tenant_isolation
ON orders
USING (tenant_id = current_setting('app.current_tenant', true))
WITH CHECK (tenant_id = current_setting('app.current_tenant', true));
```

Antes das operações de negócio, a aplicação associa o tenant autenticado à transação:

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

O `WITH CHECK` também impede que a role da aplicação grave uma linha pertencente a outro tenant.

---

## 🔐 Tenants de demonstração

| Usuário | Senha | Tenant |
|---|---|---|
| `user-a` | `password` | `TENANT_A` |
| `user-b` | `password` | `TENANT_B` |
| `user-c` | `password` | `TENANT_C` |

> ⚠️ As credenciais são utilizadas exclusivamente para demonstração e desenvolvimento local.

---

## 📦 Pedidos de demonstração

```text
TENANT_A
├── Pedido A-001
└── Pedido A-002

TENANT_B
└── Pedido B-001

TENANT_C
├── Pedido C-001
└── Pedido C-002
```

---

## 🐳 Executando com Docker

### Pré-requisitos

- Docker Desktop
- Docker Compose

```bash
git clone https://github.com/juceliocoelho2022/tenantguard-java.git
cd tenantguard-java
docker compose up --build
```

Aplicação:

```text
http://localhost:8081
```

---

## 🔑 Autenticação

### Login do Tenant A

```http
POST /api/auth/login
```

```json
{
  "username": "user-a",
  "password": "password"
}
```

Exemplo de resposta:

```json
{
  "accessToken": "<JWT_TOKEN>",
  "tokenType": "Bearer",
  "tenantId": "TENANT_A"
}
```

O JWT contém o tenant associado ao usuário autenticado.

---

## 📋 Consultando pedidos

```http
GET /api/orders
Authorization: Bearer <TOKEN>
```

Com um token de `TENANT_A`, o resultado esperado contém somente:

```json
[
  {
    "id": 1,
    "description": "Pedido A-001"
  },
  {
    "id": 2,
    "description": "Pedido A-002"
  }
]
```

---

## 🛡️ Teste de isolamento entre tenants

O pedido de ID `3` pertence ao `TENANT_B`.

```text
TENANT_A ──► /api/orders/3 ──► 404 Not Found
TENANT_B ──► /api/orders/3 ──► 200 OK ──► Pedido B-001
```

Além do filtro na aplicação, testes específicos comprovam que o PostgreSQL RLS:

- filtra linhas por tenant mesmo quando uma consulta SQL não possui predicate explícito de `tenant_id`;
- impede inserções cross-tenant através da política `WITH CHECK`.

Isso protege contra **acesso horizontal indevido entre tenants** e reduz o impacto de uma consulta que esqueça acidentalmente o filtro de tenant.

---

## 🧪 Testes automatizados e CI

Os testes utilizam PostgreSQL real em container através do Testcontainers.

```bash
mvn test
```

O projeto possui testes de isolamento na API e testes específicos da camada RLS.

O workflow **TenantGuard CI** executa automaticamente o build e os testes a cada `push` e `pull request` na branch `main`.

```text
Push / Pull Request
        │
        ▼
GitHub Actions
        │
        ▼
Java 21 + Maven
        │
        ▼
Testcontainers + PostgreSQL
        │
        ▼
Integration Tests
        │
        ▼
✅ Build validado
```

---

## 🔒 Decisões de segurança

### Tenant não controlado pelo cliente

O `tenantId` não é recebido através de query string ou body. O tenant é obtido a partir do JWT validado.

### JWT com `tenant_id`

O claim `tenant_id` determina o contexto do tenant durante a requisição.

### TenantContext

O contexto utiliza `ThreadLocal` e é obrigatoriamente limpo ao final da requisição:

```java
try {
    filterChain.doFilter(request, response);
} finally {
    TenantContext.clear();
}
```

### Consultas tenant-aware

Consultas individuais utilizam:

```text
findByIdAndTenantId(id, tenantId)
```

### RLS no banco de dados

Mesmo com consultas tenant-aware, o PostgreSQL aplica sua própria política de isolamento. Isso implementa **defesa em profundidade**.

### 404 para recursos de outros tenants

A API retorna `404 Not Found` em vez de revelar a existência de um recurso pertencente a outra organização.

---

## 📁 Estrutura conceitual

```text
src
├── main
│   ├── java/com/jucelio/tenantguard
│   │   ├── auth
│   │   ├── config
│   │   ├── order
│   │   ├── security
│   │   └── tenant
│   └── resources/db/migration
└── test/java/com/jucelio/tenantguard

.github
└── workflows
    └── ci.yml
```

---

## ⚠️ Escopo do projeto

Este projeto é uma **Proof of Concept (PoC)** criada para estudo e demonstração de arquitetura Multi-Tenant segura.

Ele não deve ser considerado uma implementação SaaS pronta para produção. Em produção seriam necessários mecanismos adicionais de gestão de secrets, identidade centralizada, observabilidade, auditoria, autorização e hardening de infraestrutura.

---

## 🚀 Roadmap técnico

- [x] JWT com `tenant_id`
- [x] TenantContext
- [x] Consultas tenant-aware
- [x] Testes de isolamento com Testcontainers
- [x] GitHub Actions CI
- [x] PostgreSQL Row Level Security — RLS
- [ ] RBAC — Role-Based Access Control
- [ ] Refresh Token
- [ ] Keycloak
- [ ] OAuth 2.0 / OpenID Connect
- [ ] Auditoria por tenant
- [ ] Rate Limiting
- [ ] Redis com isolamento por tenant
- [ ] OpenAPI / Swagger
- [ ] OpenTelemetry
- [ ] Prometheus / Grafana
- [ ] CD / Deploy automatizado
- [ ] Schema-per-Tenant
- [ ] Database-per-Tenant

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**

Java Backend Developer

`Java` • `Spring Boot` • `APIs REST` • `PostgreSQL` • `Docker` • `Spring Security` • `JWT` • `Testcontainers` • `Multi-Tenant Architecture` • `PostgreSQL RLS`

---

## 📄 Aviso

As credenciais, usuários e configurações de segurança presentes neste projeto são destinados **exclusivamente para demonstração e ambiente local**.

Não utilize secrets, senhas ou credenciais demonstrativas deste projeto em ambientes de produção.
