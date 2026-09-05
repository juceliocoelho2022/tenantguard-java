output "eks_cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS API endpoint."
  value       = module.eks.cluster_endpoint
  sensitive   = true
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint."
  value       = aws_db_instance.postgres.address
}

output "redis_primary_endpoint" {
  description = "ElastiCache primary endpoint."
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "database_secret_arn" {
  description = "Secrets Manager ARN containing database connection metadata."
  value       = aws_secretsmanager_secret.database.arn
}

output "redis_secret_arn" {
  description = "Secrets Manager ARN containing Redis connection metadata."
  value       = aws_secretsmanager_secret.redis.arn
}
