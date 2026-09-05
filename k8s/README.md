# TenantGuard on Kubernetes

This directory contains a production-oriented starting point for running TenantGuard on Kubernetes with health probes, resource controls and horizontal autoscaling.

## Health probes

- `startupProbe`: `/actuator/health/liveness`
- `livenessProbe`: `/actuator/health/liveness`
- `readinessProbe`: `/actuator/health/readiness`

The readiness endpoint includes the database health contributor, so a pod is removed from service when the application cannot reach PostgreSQL. Liveness checks only the application process state and must not depend on an external database.

## Resource requests and limits

`deployment.yaml` defines CPU and memory requests/limits for each TenantGuard pod. These values protect cluster capacity and provide the resource baseline used by the Horizontal Pod Autoscaler.

Current defaults:

- CPU request: `250m`
- CPU limit: `1000m`
- Memory request: `256Mi`
- Memory limit: `768Mi`

Tune these values from production telemetry rather than treating them as final sizing numbers.

## Horizontal Pod Autoscaler

`hpa.yaml` uses `autoscaling/v2` and scales the `tenantguard-app` Deployment between 2 and 6 replicas.

Scaling targets:

- CPU average utilization: `70%`
- Memory average utilization: `75%`
- Scale-up may double capacity or add up to 2 pods per minute, whichever is larger
- Scale-down uses a 5-minute stabilization window and reduces capacity gradually

A Kubernetes metrics provider is required. On standard clusters this is typically Metrics Server. Managed Kubernetes platforms may already provide or integrate this capability.

Validate metrics before relying on HPA:

```bash
kubectl top pods
kubectl get hpa tenantguard-app
kubectl describe hpa tenantguard-app
```

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

For multi-replica production deployments, configure the authentication rate limiter to use the Redis backend and point the application to a production-grade Redis service.

## Apply

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

## Validate

```bash
kubectl get pods
kubectl describe pod -l app=tenantguard-app
kubectl get service tenantguard-service
kubectl get hpa tenantguard-app
```

For production, use managed PostgreSQL and Redis services where appropriate, a secrets manager/external-secrets integration, a real container registry, TLS ingress, metrics infrastructure, alerting and deployment automation.
