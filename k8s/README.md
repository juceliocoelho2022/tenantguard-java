# TenantGuard on Kubernetes

This directory contains a production-oriented starting point for running TenantGuard with Kubernetes health probes.

## Health probes

- `startupProbe`: `/actuator/health/liveness`
- `livenessProbe`: `/actuator/health/liveness`
- `readinessProbe`: `/actuator/health/readiness`

The readiness endpoint includes the database health contributor, so a pod is removed from service when the application cannot reach PostgreSQL. Liveness checks only the application process state and must not depend on an external database.

## Required environment-specific values

Before deploying, replace `CHANGE_ME` in `configmap.yaml` with the PostgreSQL host used by the target environment and replace the image in `deployment.yaml` with the image published by your registry pipeline.

Create the required secret outside Git:

```bash
kubectl create secret generic tenantguard-secrets \
  --from-literal=SPRING_DATASOURCE_USERNAME='<database-user>' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<database-password>' \
  --from-literal=JWT_SECRET='<long-random-jwt-secret>'
```

Do not commit real database credentials or JWT secrets to this repository.

## Apply

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

## Validate

```bash
kubectl get pods
kubectl describe pod -l app=tenantguard-app
kubectl get service tenantguard-service
```

For production, use a managed PostgreSQL service or separately managed database infrastructure, a secrets manager/external-secrets integration, a real container registry, TLS ingress, and distributed rate limiting rather than the current per-instance in-memory limiter.
