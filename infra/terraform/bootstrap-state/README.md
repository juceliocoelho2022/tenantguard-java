# Terraform Remote State Bootstrap

This directory creates the S3 bucket used by the main TenantGuard Terraform configuration.

It intentionally uses local state during bootstrap because the remote backend cannot reference a bucket that does not exist yet.

## 1. Create the state bucket

```bash
cd infra/terraform/bootstrap-state
terraform init
terraform plan
terraform apply
terraform output -raw state_bucket_name
```

The bucket is configured with:

- S3 versioning
- server-side encryption
- blocked public access
- bucket-owner-enforced object ownership
- a bucket policy denying non-TLS requests
- `prevent_destroy = true`
- `force_destroy = false`

The bootstrap state does not contain application database credentials. Keep the local bootstrap state protected and do not commit it.

## 2. Configure the main backend

From `infra/terraform`, copy the example backend configuration:

```bash
cp backend.hcl.example backend.hcl
```

Set the real bucket returned by the bootstrap output and the AWS region.

`backend.hcl` is ignored by Git so environment-specific backend values are not committed.

## 3. Migrate the existing main state

```bash
terraform init -migrate-state -backend-config=backend.hcl
```

Terraform will move the main state to S3. The main backend enables S3 native state locking through `use_lockfile = true`.

## Recovery

S3 versioning is enabled so prior state object versions can be recovered after accidental overwrites or deletion. Treat the state as sensitive because Terraform state can contain secret values even when Terraform outputs are marked sensitive.
