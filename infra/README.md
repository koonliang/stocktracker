# Infrastructure (Terraform)

Manages the AWS and Cloudflare resources for StockTracker.

## Layout

```
infra/
├── bootstrap/            # One-time, manually applied. Creates Terraform state
│                          # backend (S3 + DynamoDB) and GitHub OIDC + IAM roles.
├── envs/
│   └── production/       # The single production environment for v1.
│                          # All other env/region targets are out of scope.
└── modules/              # Reusable building blocks (network, lambda, rds, etc.)
```

Module documentation lives next to the module's `main.tf`.

## One-time bootstrap

The bootstrap stack uses local state. Run it once per AWS account to provision
the things Terraform itself needs: the remote-state bucket, the lock table,
the GitHub OIDC provider, and the two IAM roles (`gha-plan-production` and
`gha-deploy-production`).

```bash
cd infra/bootstrap
terraform init
terraform apply \
  -var "github_org=koonliang" \
  -var "github_repo=stocktracker" \
  -var "aws_region=ap-southeast-1"
```

Record the outputs (`state_bucket_name`, `lock_table_name`, the two role ARNs)
into:

- `infra/envs/production/backend.tf` — replace the placeholder values for the
  state bucket and lock table.
- GitHub repository variables: `AWS_REGION`, `AWS_PLAN_ROLE_ARN`,
  `AWS_DEPLOY_ROLE_ARN`.

## First-time provisioning of `production`

```bash
cd infra/envs/production
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

Useful variables (see `variables.tf` for the full list):

- `aws_region` (default `ap-southeast-1`)
- `domain_name` (optional; leave empty to skip the API custom domain and
  Cloudflare wiring — uses the API Gateway default invoke URL)
- `acm_certificate_arn` (optional; required only when `domain_name` is set)
- `provisioned_concurrency` (default `0`)

## Required GitHub branch protection (configure once, manually)

On `main`:

- Require pull-request review before merge.
- Require status check `gates` to pass.
- Require branches to be up to date before merging.

## Environments

Only `production` exists in v1. To add a new environment later, copy
`envs/production/` to `envs/<new-name>/`, override variables, and run
`terraform init && terraform apply`. **Module changes should not be needed**
for additional environments.

## What changes if `domain_name` and `acm_certificate_arn` are empty

- API Gateway is reachable at the AWS auto-issued
  `https://<api-id>.execute-api.<region>.amazonaws.com/`.
- Cloudflare module is **not** wired (no proxied DNS, no transform rule for
  the `X-Origin-Auth` header on the S3 bucket).
- Frontend is served either from the S3 website endpoint (test mode) or
  whatever fallback you configure. **Note that this violates FR-009** in
  the spec (S3 must not be publicly readable in production); use the
  no-domain mode only for ephemeral test cycles.
