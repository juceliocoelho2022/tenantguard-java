resource "aws_iam_role" "load_balancer_controller" {
  name = "${local.name}-load-balancer-controller"

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
}

resource "aws_iam_policy" "load_balancer_controller" {
  name        = "${local.name}-load-balancer-controller"
  description = "AWS Load Balancer Controller v2.14.1 permissions"
  policy      = file("${path.module}/policies/aws-load-balancer-controller-v2.14.1.json")
}

resource "aws_iam_role_policy_attachment" "load_balancer_controller" {
  role       = aws_iam_role.load_balancer_controller.name
  policy_arn = aws_iam_policy.load_balancer_controller.arn
}

resource "aws_eks_pod_identity_association" "load_balancer_controller" {
  cluster_name    = module.eks.cluster_name
  namespace       = "kube-system"
  service_account = "aws-load-balancer-controller"
  role_arn        = aws_iam_role.load_balancer_controller.arn
}

resource "aws_iam_role" "github_actions_cluster_bootstrap" {
  name = "${local.name}-github-actions-cluster-bootstrap"

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
          "token.actions.githubusercontent.com:sub" = "repo:juceliocoelho2022/tenantguard-java:environment:production-bootstrap"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_actions_cluster_bootstrap" {
  name = "${local.name}-github-actions-cluster-bootstrap"
  role = aws_iam_role.github_actions_cluster_bootstrap.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["eks:DescribeCluster"]
      Resource = module.eks.cluster_arn
    }]
  })
}

resource "aws_eks_access_entry" "github_actions_cluster_bootstrap" {
  cluster_name  = module.eks.cluster_name
  principal_arn = aws_iam_role.github_actions_cluster_bootstrap.arn
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "github_actions_cluster_bootstrap" {
  cluster_name  = module.eks.cluster_name
  principal_arn = aws_iam_role.github_actions_cluster_bootstrap.arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.github_actions_cluster_bootstrap]
}
