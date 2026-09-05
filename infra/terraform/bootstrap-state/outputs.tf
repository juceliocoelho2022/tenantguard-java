output "state_bucket_name" {
  description = "S3 bucket used by the main Terraform configuration for remote state."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "backend_init_command" {
  description = "Command template for initializing the main Terraform configuration with the remote backend."
  value       = "terraform init -backend-config=backend.hcl"
}
