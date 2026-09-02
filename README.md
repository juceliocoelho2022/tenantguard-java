# 🛡️ TenantGuard Java

<p align="center">
  <img src="docs/images/tenantguard-architecture.png"
       alt="TenantGuard Java - Secure Multi-Tenant Architecture"
       width="100%">
</p>

Projeto demonstrativo de **Multi-Tenancy seguro** desenvolvido com **Java 21 e Spring Boot**, com foco em isolamento de dados entre tenants, autenticação JWT, PostgreSQL, Docker e testes de integração.
Projeto demonstrativo de **Multi-Tenancy seguro** desenvolvido com **Java 21 e Spring Boot**, com foco em isolamento de dados entre tenants, autenticação JWT, PostgreSQL, Docker e testes de integração.

O objetivo é demonstrar, por meio de código e testes automatizados, como construir uma API SaaS em que cada tenant autenticado consegue acessar **somente os próprios dados**.

---

## 🎯 Objetivo

O TenantGuard Java demonstra uma arquitetura Multi-Tenant utilizando:

**Shared Database + Shared Schema + `tenant_id`**

Nesse modelo, diferentes tenants compartilham o mesmo banco de dados e as mesmas tabelas, enquanto o isolamento dos registros é realizado através da coluna `tenant_id`.

O tenant **não é recebido do cliente** através de query string, path parameter ou body.

Ele é identificado a partir do claim `tenant_id` presente no JWT autenticado.

---

## 🏗️ Arquitetura

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
Service Layer
  │
  ▼
Repository
  │
  ▼
Hibernate / JPA
  │
  ▼
PostgreSQL
```

### Fluxo de autenticação e resolução do tenant

```text
Login
  │
  ▼
JWT gerado com claim tenant_id
  │
  ▼
Authorization: Bearer <TOKEN>
  │
  ▼
Spring Security
  │
  ▼
JwtAuthenticationFilter
  │
  ▼
JwtTenantResolver
  │
  ▼
TenantContext
  │
  ▼
Consulta filtrada pelo tenant autenticado
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

### Banco de dados

- PostgreSQL
- Flyway

### Infraestrutura

- Docker
- Docker Compose

### Testes

- JUnit 5
- MockMvc
- Testcontainers
- PostgreSQL Testcontainer

---

## 🏢 Estratégia Multi-Tenant

A estratégia utilizada nesta versão é:

```text
Shared Database
      +
Shared Schema
      +
tenant_id
```

Exemplo conceitual da tabela de pedidos:

| id | description | tenant_id |
|---:|---|---|
| 1 | Pedido A-001 | TENANT_A |
| 2 | Pedido A-002 | TENANT_A |
| 3 | Pedido B-001 | TENANT_B |
| 4 | Pedido C-001 | TENANT_C |
| 5 | Pedido C-002 | TENANT_C |

Mesmo compartilhando a mesma tabela, cada tenant deve visualizar somente seus próprios registros.

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

O banco é inicializado com dados pertencentes aos três tenants.

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

Isso permite demonstrar e testar o isolamento entre tenants.

---

## 🐳 Executando com Docker

### Pré-requisitos

Tenha instalado:

- Docker Desktop
- Docker Compose

Clone o projeto:

```bash
git clone https://github.com/juceliocoelho2022/tenantguard-java.git
```

Entre na pasta:

```bash
cd tenantguard-java
```

Execute:

```bash
docker compose up --build
```

A aplicação ficará disponível em:

```text
http://localhost:8081
```

O PostgreSQL também será iniciado automaticamente pelo Docker Compose.

---

## 🔑 Autenticação

### Login do Tenant A

Endpoint:

```http
POST /api/auth/login
```

Body:

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

Após realizar o login, envie o JWT através do header:

```http
Authorization: Bearer <TOKEN>
```

Endpoint:

```http
GET /api/orders
```

Utilizando um token pertencente ao `TENANT_A`, o resultado esperado é:

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

O pedido pertencente ao `TENANT_B` não aparece na consulta.

---

## 🛡️ Teste de isolamento entre tenants

Este é o cenário principal demonstrado pelo projeto.

O pedido:

```text
ID: 3
Descrição: Pedido B-001
Tenant: TENANT_B
```

pertence ao `TENANT_B`.

### Tentativa utilizando TENANT_A

```http
GET /api/orders/3
Authorization: Bearer <TOKEN_TENANT_A>
```

Resultado esperado:

```text
404 Not Found
```

O recurso não é retornado porque pertence a outro tenant.

### Tentativa utilizando TENANT_B

Com um JWT pertencente ao `TENANT_B`:

```http
GET /api/orders/3
Authorization: Bearer <TOKEN_TENANT_B>
```

Resultado esperado:

```text
200 OK
```

Resposta:

```json
{
  "id": 3,
  "description": "Pedido B-001"
}
```

Dessa forma, o mesmo recurso:

```text
TENANT_A ──► /api/orders/3 ──► 404 Not Found

TENANT_B ──► /api/orders/3 ──► 200 OK
                                  │
                                  └── Pedido B-001
```

Isso demonstra a proteção contra **acesso horizontal indevido entre tenants**.

---

## 🧪 Testes automatizados

O projeto possui testes de integração para validar o isolamento Multi-Tenant.

Com o Docker disponível, execute:

```bash
mvn test
```

Os testes utilizam:

- JUnit 5
- MockMvc
- Testcontainers
- PostgreSQL real executado em container

### Resultado validado

```text
Tests run: 3
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Os testes comprovam que:

1. `TENANT_A` lista somente seus próprios pedidos.
2. `TENANT_A` não consegue acessar o pedido pertencente ao `TENANT_B`.
3. `TENANT_B` consegue acessar normalmente seu próprio pedido.

---

## 🔒 Decisões de segurança

Algumas decisões importantes adotadas no projeto:

### Tenant não controlado pelo cliente

O `tenantId` não é recebido através de:

```text
?tenantId=TENANT_A
```

nem através do body da requisição.

O tenant é obtido a partir do JWT validado.

---

### JWT com tenant_id

Após a autenticação, o JWT contém o claim:

```text
tenant_id
```

Esse valor é utilizado para determinar o contexto do tenant durante a requisição.

---

### TenantContext

O tenant autenticado é armazenado temporariamente em um `TenantContext`.

O contexto utiliza `ThreadLocal` para manter o tenant associado à execução atual.

Ao final da requisição, o contexto é obrigatoriamente limpo.

Exemplo conceitual:

```java
try {
    filterChain.doFilter(request, response);
} finally {
    TenantContext.clear();
}
```

Isso reduz o risco de vazamento de contexto entre requisições.

---

### Consultas protegidas

Consultas individuais utilizam a combinação:

```text
id + tenant_id
```

em vez de procurar apenas pelo identificador do recurso.

Conceitualmente:

```text
findByIdAndTenantId(id, tenantId)
```

Assim, conhecer o ID de um recurso pertencente a outro tenant não é suficiente para acessá-lo.

---

### 404 para recursos de outros tenants

Quando um tenant tenta consultar um recurso pertencente a outro tenant, a API retorna:

```text
404 Not Found
```

em vez de revelar que o recurso existe para outra organização.

---

## 📁 Estrutura conceitual

```text
src
├── main
│   ├── java
│   │   └── com.jucelio.tenantguard
│   │       ├── auth
│   │       ├── config
│   │       ├── order
│   │       ├── security
│   │       └── tenant
│   │
│   └── resources
│       └── db
│           └── migration
│
└── test
    └── java
        └── com.jucelio.tenantguard
```

---

## 🔄 Fluxo completo

```text
                ┌─────────────────┐
                │     CLIENT      │
                └────────┬────────┘
                         │
                         │ Login
                         ▼
                ┌─────────────────┐
                │ Spring Security │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │       JWT       │
                │    tenant_id    │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │JwtTenantResolver│
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │  TenantContext  │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Service Layer   │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │   Repository    │
                │ id + tenant_id  │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Hibernate / JPA │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │   PostgreSQL    │
                └─────────────────┘
```

---

## ⚠️ Escopo do projeto

Este projeto é uma **Proof of Concept (PoC)** criada para estudo e demonstração de arquitetura Multi-Tenant.

Ele não deve ser considerado uma implementação SaaS pronta para produção.

Alguns mecanismos adicionais seriam necessários em um ambiente produtivo, incluindo gestão segura de secrets, identidade centralizada, observabilidade, auditoria, políticas no banco e controles adicionais de autorização.

---

## 🚀 Próximas evoluções

Roadmap técnico:

- [ ] PostgreSQL Row Level Security — RLS
- [ ] RBAC — Role-Based Access Control
- [ ] Refresh Token
- [ ] Keycloak
- [ ] OAuth 2.0
- [ ] OpenID Connect
- [ ] Auditoria por tenant
- [ ] Rate Limiting
- [ ] Redis com isolamento por tenant
- [ ] OpenAPI / Swagger
- [ ] OpenTelemetry
- [ ] Prometheus
- [ ] Grafana
- [ ] GitHub Actions
- [ ] CI/CD
- [ ] Schema-per-Tenant
- [ ] Database-per-Tenant

---

## 💡 Próxima camada de segurança

Uma das principais evoluções planejadas é implementar:

**PostgreSQL Row Level Security (RLS)**

A arquitetura passará a utilizar defesa em profundidade:

```text
JWT
 ↓
TenantResolver
 ↓
TenantContext
 ↓
Repository
 ↓
Hibernate
 ↓
PostgreSQL RLS
```

Assim, além da aplicação controlar o acesso aos registros, o próprio PostgreSQL poderá aplicar políticas de isolamento entre tenants.

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**

Java Backend Developer

Tecnologias e áreas de interesse:

`Java` • `Spring Boot` • `APIs REST` • `PostgreSQL` • `Docker` • `Spring Security` • `JWT` • `Testcontainers` • `Multi-Tenant Architecture`

---

## 📄 Aviso

As credenciais, usuários e configurações de segurança presentes neste projeto são destinados **exclusivamente para demonstração e ambiente local**.

Não utilize secrets, senhas ou credenciais demonstrativas deste projeto em ambientes de produção.