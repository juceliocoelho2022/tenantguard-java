variable "aws_region" {
  description = "AWS region for TenantGuard production resources."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name used for resource naming."
  type        = string
  default     = "tenantguard"
}

variable "vpc_cidr" {
  description = "CIDR block for the production VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "eks_cluster_version" {
  description = "Kubernetes version for EKS. Keep this aligned with AWS-supported versions before apply."
  type        = string
  default     = "1.34"
}

variable "eks_node_instance_types" {
  description = "EC2 instance types for the default EKS managed node group."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "rds_instance_class" {
  description = "RDS PostgreSQL instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "rds_engine_version" {
  description = "PostgreSQL engine major version."
  type        = string
  default     = "17"
}

variable "redis_node_type" {
  description = "ElastiCache Redis/Valkey node type."
  type        = string
  default     = "cache.t4g.micro"
}

variable "db_name" {
  description = "TenantGuard database name."
  type        = string
  default     = "tenantguard"
}

variable "db_username" {
  description = "TenantGuard database administrator username."
  type        = string
  default     = "tenantguard_admin"
}
