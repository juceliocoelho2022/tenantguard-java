terraform {
  backend "s3" {
    key          = "tenantguard/prod/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}
