# Contract: GitHub → AWS OIDC Trust

This contract pins the trust relationship between the GitHub Actions OIDC
provider and the AWS IAM deploy role. Any change here must be reviewed
because it affects what GitHub workflows can do in AWS.

## OIDC Provider (one-time)

- **URL**: `https://token.actions.githubusercontent.com`
- **Client ID**: `sts.amazonaws.com`
- **Thumbprint**: managed by AWS (modern AWS accounts no longer pin
  thumbprints; configured via `aws_iam_openid_connect_provider`)

## IAM Role: `gha-deploy-production`

### Trust policy (subject claims)

The role is assumable **only** from this repository, and only by these
trigger contexts:

| Trigger | `sub` claim pattern | What it can do |
|---------|---------------------|----------------|
| `push` to `main` | `repo:<org>/<repo>:ref:refs/heads/main` | full deploy (apply + deploy backend + deploy frontend) |
| `pull_request` from same-repo branch | `repo:<org>/<repo>:pull_request` | `terraform plan` only — see permissions below |
| `workflow_dispatch` (rollback) | `repo:<org>/<repo>:ref:refs/heads/main` | same as `push:main` |

Forks cannot match either pattern — they always fail trust evaluation —
which is what enforces FR-005 at the IAM level (defense-in-depth on top of
the workflow-level fork check).

### Permissions

The role has **two** attached policies:

1. **Plan-time policy** (`AWSReadOnlyAccess`-like, scoped):
   - `s3:Get*`, `s3:List*` on the Terraform state bucket
   - `dynamodb:GetItem`, `PutItem`, `DeleteItem` on the lock table (required
     even for plan, because Terraform takes a read lock)
   - `Describe*` / `Get*` / `List*` on the AWS services we manage
2. **Apply-time policy** (full management of *only* the resources Terraform
   owns):
   - Scoped by resource ARN patterns where possible (e.g.,
     `arn:aws:lambda:<region>:<acct>:function:stocktracker-*`)
   - `iam:PassRole` scoped to the Lambda execution roles by ARN

The trust policy gates which policy applies: the apply-time policy is only
attachable to sessions whose subject claim matches a `main`-branch context.
At runtime this is implemented by **two separate roles**:

- `gha-plan-production` — assumed by PR jobs; read-only plus state-lock
  write.
- `gha-deploy-production` — assumed by `main`-branch jobs; full apply
  permissions.

Workflows reference them by repository variables `AWS_PLAN_ROLE_ARN` and
`AWS_DEPLOY_ROLE_ARN`.

## What a contract violation looks like

- A workflow that assumes `gha-deploy-production` from a non-`main` ref
  context — should fail at AWS STS.
- A trust policy with `repo:*` or any wildcard in the org/repo segment of
  `sub` — must be rejected in PR review.
- Long-lived IAM user access keys for deployment — explicitly forbidden by
  FR-024.
