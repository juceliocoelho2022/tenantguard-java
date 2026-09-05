variable "aws_region" {
  description = "AWS region that will host the Terraform state bucket."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used in the state bucket naming convention."
  type        = string
  default     = "tenantguard"
}

variable "environment" {
  description = "Environment name used in the state bucket naming convention."
  type        = string
  default     = "prod"
}
