# TenantGuard on Kubernetes

This directory contains a production-oriented starting point for running TenantGuard on Kubernetes with health probes, resource controls, horizontal autoscaling and AWS Secrets Manager integration.

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

A Kubernetes metrics provider is required. On EKS, the Terraform stack enables Metrics Server.

Validate metrics before relying on HPA:

```bash
kubectl top pods
kubectl get hpa tenantguard-app
kubectl describe hpa tenantguard-app
```

## AWS Secrets Manager integration

Production database and Redis connection data are stored in AWS Secrets Manager by Terraform.

The application pod uses:

- EKS Pod Identity via the `tenantguard-app` ServiceAccount
- Secrets Store CSI Driver with the AWS provider
- `SecretProviderClass` in `k8s/aws/secret-provider-class.yaml`
- a read-only CSI volume mounted at `/mnt/secrets-store`
- Spring Boot `configtree:` loading via `SPRING_CONFIG_IMPORT`

The mounted files are named as Spring properties:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.data.redis.host`
- `spring.data.redis.port`
- `spring.data.redis.ssl.enabled`

This avoids copying RDS or Redis credentials into a Kubernetes Secret.

`tenantguard-secrets` is still required for application secrets that are not currently provisioned by Terraform, such as `JWT_SECRET`:

```bash
kubectl create secret generic tenantguard-secrets \
  --from-literal=JWT_SECRET='<long-random-jwt-secret>'
```

Do not commit real credentials or JWT secrets to this repository.

## Apply on EKS

Apply the AWS-specific identity and secret mapping before the Deployment:

```bash
kubectl apply -f k8s/aws/service-account.yaml
kubectl apply -f k8s/aws/secret-provider-class.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

Replace the image in `deployment.yaml` with the image published by the registry pipeline before production rollout.

## Validate

```bash
kubectl get pods
kubectl describe pod -l app=tenantguard-app
kubectl get service tenantguard-service
kubectl get hpa tenantguard-app
kubectl exec deploy/tenantguard-app -- ls -la /mnt/secrets-store
```

Do not print mounted secret file contents during validation.

For production, also use a real container registry, TLS ingress, metrics infrastructure, alerting and deployment automation.
