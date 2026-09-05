resource "aws_iam_role" "github_actions_runtime_secrets_bootstrap" {
  name = "${local.name}-github-actions-runtime-secrets-bootstrap"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github_actions.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = "repo:juceliocoelho2022/tenantguard-java:environment:production-secrets"
        }
      }
    }]
  })

  tags = local.common_tags
}

resource "aws_iam_role_policy" "github_actions_runtime_secrets_bootstrap" {
  name = "${local.name}-runtime-secrets-bootstrap"
  role = aws_iam_role.github_actions_runtime_secrets_bootstrap.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "secretsmanager:DescribeSecret",
        "secretsmanager:GetSecretValue",
        "secretsmanager:PutSecretValue"
      ]
      Resource = aws_secretsmanager_secret.jwt.arn
    }]
  })
}
