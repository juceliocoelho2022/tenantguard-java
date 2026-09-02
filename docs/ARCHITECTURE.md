# Arquitetura

## Estratégia v1

Shared Database + Shared Schema.

A coluna `tenant_id` define a propriedade lógica de cada registro.

## Regra central

O cliente nunca escolhe o tenant da consulta.

```text
Authorization: Bearer JWT
           ↓
JwtTenantResolver
           ↓
claim tenant_id
           ↓
TenantContext
           ↓
OrderService
           ↓
findByIdAndTenantId(...)
```

A próxima evolução recomendada é complementar a proteção da aplicação com PostgreSQL Row Level Security.
