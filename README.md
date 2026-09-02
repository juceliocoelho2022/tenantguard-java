# TenantGuard Java

Projeto demonstrativo de **Multi-Tenancy seguro** com Java e Spring Boot.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Docker / Docker Compose
- JUnit 5
- MockMvc
- Testcontainers

## Objetivo

Provar, por código e testes automatizados, que um tenant autenticado só consegue acessar os próprios dados.

Arquitetura adotada nesta primeira versão:

**Shared Database + Shared Schema + `tenant_id`**

```text
Login
  ↓
JWT com tenant_id
  ↓
Spring Security
  ↓
JwtTenantResolver
  ↓
TenantContext
  ↓
Service / Repository
  ↓
Hibernate
  ↓
PostgreSQL
```

## Tenants de demonstração

| Usuário | Senha | Tenant |
|---|---|---|
| user-a | password | TENANT_A |
| user-b | password | TENANT_B |
| user-c | password | TENANT_C |

## Rodar com Docker

```bash
docker compose up --build
```

API em:

```text
http://localhost:8080
```

## Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user-a",
  "password": "password"
}
```

## Consultar pedidos

```http
GET /api/orders
Authorization: Bearer <TOKEN>
```

## Teste crítico

O pedido `3` pertence ao `TENANT_B`.

Com JWT do `TENANT_A`:

```http
GET /api/orders/3
Authorization: Bearer <TOKEN_TENANT_A>
```

Resultado esperado:

```text
404 Not Found
```

Com JWT do `TENANT_B`, o mesmo recurso retorna `200 OK`.

## Testes automatizados

Com Docker em execução:

```bash
mvn test
```

Os testes comprovam que:

- Tenant A lista apenas seus próprios pedidos.
- Tenant A não acessa o pedido do Tenant B.
- Tenant B acessa normalmente seu próprio pedido.

## Decisões de segurança

- O `tenantId` não vem da query string nem do body.
- O tenant vem do claim `tenant_id` do JWT validado.
- Consultas individuais usam `id + tenant_id`.
- Recurso de outro tenant retorna `404`.
- `TenantContext` é limpo no `finally` do filtro.

## Próximas evoluções

- PostgreSQL Row Level Security (RLS)
- Schema-per-tenant
- Database-per-tenant
- Redis por tenant
- auditoria por tenant
- rate limiting
- OpenTelemetry + Prometheus + Grafana
- Keycloak / OAuth 2.0 / OpenID Connect
- GitHub Actions

> As credenciais e o segredo JWT deste projeto são apenas para demonstração.
