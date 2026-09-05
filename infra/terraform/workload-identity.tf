resource "aws_iam_role" "tenantguard_app" {
  name = "${local.name}-app-pod-identity"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "pods.eks.amazonaws.com"
      }
      Action = [
        "sts:AssumeRole",
        "sts:TagSession"
      ]
    }]
  })

  tags = local.common_tags
}

resource "aws_iam_policy" "tenantguard_secrets" {
  name        = "${local.name}-secrets-read"
  description = "Least-privilege access to TenantGuard runtime secrets"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ]
      Resource = [
        aws_secretsmanager_secret.database.arn,
        aws_secretsmanager_secret.redis.arn
      ]
    }]
  })
}

resource "aws_iam_role_policy_attachment" "tenantguard_secrets" {
  role       = aws_iam_role.tenantguard_app.name
  policy_arn = aws_iam_policy.tenantguard_secrets.arn
}

resource "aws_eks_pod_identity_association" "tenantguard_app" {
  cluster_name    = module.eks.cluster_name
  namespace       = "default"
  service_account = "tenantguard-app"
  role_arn        = aws_iam_role.tenantguard_app.arn
}
