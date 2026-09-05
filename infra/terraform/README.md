# TenantGuard — AWS Production Architecture

Sprint 9 introduced the production-oriented AWS foundation. Sprint 10 extends it with secure image delivery, controlled EKS rollout and ALB ingress bootstrapping.

## Architecture

```text
GitHub Actions
      |
      | OIDC
      v
AWS IAM delivery role
      |
      v
Amazon ECR
      |
      v
Amazon EKS
      |
      +--> AWS Load Balancer Controller
      |          |
      |          v
      |       ALB / HTTPS
      |
      +--> TenantGuard Pods
              |       \
              v        v
         Amazon RDS  ElastiCache Redis
              |
              v
       AWS Secrets Manager
```

## Production delivery

The delivery workflow builds the Docker image, publishes it to ECR using an immutable Git SHA tag, updates the Kubernetes Deployment and waits for a successful rollout.

Required GitHub repository variables:

- `AWS_REGION`
- `AWS_ROLE_ARN` — Terraform output `github_actions_delivery_role_arn`
- `ECR_REPOSITORY` — default `tenantguard-prod-app`
- `EKS_CLUSTER_NAME` — Terraform output `eks_cluster_name`
- `ENABLE_ALB_INGRESS` — set to `true` after the controller is installed
- `ACM_CERTIFICATE_ARN` — optional; when present, configures HTTPS 443 and HTTP-to-HTTPS redirect
- `ALB_SSL_POLICY` — optional; defaults to `ELBSecurityPolicy-TLS13-1-2-2021-06`

No long-lived AWS access keys are required. GitHub Actions receives short-lived AWS credentials through OIDC.

## AWS Load Balancer Controller

Terraform provisions:

- an IAM role trusted by `pods.eks.amazonaws.com`
- the official AWS Load Balancer Controller v2.14.1 IAM policy vendored under `infra/terraform/policies/`
- an EKS Pod Identity association for `kube-system/aws-load-balancer-controller`
- a separate GitHub OIDC bootstrap role
- a cluster-level EKS access entry only for the protected bootstrap workflow

The controller bootstrap is deliberately isolated from the normal application delivery role. The workflow `.github/workflows/bootstrap-alb-controller.yml` runs in the GitHub Environment `production-bootstrap` and installs chart `aws-load-balancer-controller` version `1.14.0` with Helm.

Configure the `production-bootstrap` environment with:

- `AWS_REGION`
- `EKS_CLUSTER_NAME`
- `AWS_EKS_BOOTSTRAP_ROLE_ARN` — Terraform output `github_actions_cluster_bootstrap_role_arn`

Use environment protection rules so approval is required before the cluster-admin bootstrap role can be assumed.

## ALB and HTTPS

`k8s/aws/ingress.yaml` defines an internet-facing ALB ingress using IP targets and the application readiness endpoint for health checks.

When `ENABLE_ALB_INGRESS=true`, the delivery workflow applies the Ingress. If `ACM_CERTIFICATE_ARN` is also configured, the workflow adds:

- HTTPS listener on port 443
- ACM certificate ARN
- HTTP port 80 listener
- HTTP-to-HTTPS redirect to 443
- configured TLS security policy

The certificate ARN is intentionally not committed to the repository.

## Security principles

- RDS and Redis are private.
- Application runtime credentials are read through EKS Pod Identity and Secrets Store CSI.
- GitHub uses OIDC instead of static AWS keys.
- ECR uses immutable tags and scan-on-push.
- Normal delivery access is namespace-scoped.
- Cluster-admin access is isolated to the protected `production-bootstrap` GitHub Environment.
- ALB controller permissions are attached only to its Pod Identity role.
- HTTPS uses an external ACM certificate ARN rather than a committed secret.

## Usage

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
```

Do not commit `terraform.tfvars`, Terraform state files, generated plans or credentials.

## Production notes

The current configuration intentionally uses a single NAT Gateway and Single-AZ RDS to control portfolio cost. A stricter production environment should use one NAT Gateway per Availability Zone, Multi-AZ RDS and reviewed instance sizing.

The EKS Kubernetes version is configurable. Confirm the selected version is supported in the target AWS region before apply.

The bootstrap Helm chart is pinned. Review AWS Load Balancer Controller releases before upgrading because Helm does not automatically apply future security updates.

## Next increments

1. Add Terraform remote state with encrypted S3 state locking.
2. Commit and maintain `.terraform.lock.hcl`.
3. Add static Terraform security checks to CI.
4. Move the JWT signing secret to AWS Secrets Manager.
5. Harden production PostgreSQL migration/runtime role separation for RLS.
6. Review private-cluster VPC endpoints before restricting EKS public API access.
