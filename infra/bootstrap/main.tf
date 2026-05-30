terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "stocktracker"
      Environment = "bootstrap"
      ManagedBy   = "terraform"
    }
  }
}

data "aws_caller_identity" "current" {}

locals {
  account_id   = data.aws_caller_identity.current.account_id
  state_bucket = "stocktracker-tfstate-${local.account_id}-${var.aws_region}"
  lock_table   = "stocktracker-tflock"
  repo_subject = "repo:${var.github_org}/${var.github_repo}"
}

# ------------------------------------------------------------------------------
# Remote state backend
# ------------------------------------------------------------------------------

resource "aws_s3_bucket" "tfstate" {
  bucket = local.state_bucket
}

resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "tflock" {
  name         = local.lock_table
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }
}

# ------------------------------------------------------------------------------
# GitHub OIDC provider
#
# The OIDC provider is account-wide. To stay idempotent across other projects
# that may already have created it (and to avoid AWS's EntityAlreadyExists),
# we look it up as a data source. If the provider does not exist yet, create
# it manually once with:
#
#   aws iam create-open-id-connect-provider \
#     --url https://token.actions.githubusercontent.com \
#     --client-id-list sts.amazonaws.com
#
# (Modern AWS accounts no longer require thumbprints.)
# ------------------------------------------------------------------------------

data "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
}

# ------------------------------------------------------------------------------
# Plan-time role: assumed by PR jobs (read-only + state-lock writes only).
# ------------------------------------------------------------------------------

data "aws_iam_policy_document" "gha_plan_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [data.aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # Only PR workflow runs on this repo can assume this role.
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.repo_subject}:pull_request"]
    }
  }
}

resource "aws_iam_role" "gha_plan" {
  name               = "gha-plan-production"
  assume_role_policy = data.aws_iam_policy_document.gha_plan_trust.json
}

# Read-only across all AWS services is provided by AWS's managed policy.
# (IAM action wildcards in custom policies must name a specific service
# prefix — bare `Get*` and `*:Get*` both fail validation, so we lean on
# the managed policy instead of recreating it.)
resource "aws_iam_role_policy_attachment" "gha_plan_readonly" {
  role       = aws_iam_role.gha_plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

# Plan also needs to acquire the state lock (writes to the DynamoDB lock
# table) and read/write the per-run state object in S3. These are the only
# writes the plan-time role is allowed to perform.
data "aws_iam_policy_document" "gha_plan_state_lock" {
  statement {
    sid    = "StateLockWrites"
    effect = "Allow"

    actions = [
      "dynamodb:PutItem",
      "dynamodb:DeleteItem",
      "s3:PutObject",
      "s3:DeleteObject"
    ]

    resources = [
      aws_dynamodb_table.tflock.arn,
      "${aws_s3_bucket.tfstate.arn}/*"
    ]
  }
}

resource "aws_iam_role_policy" "gha_plan_state_lock" {
  name   = "state-lock-writes"
  role   = aws_iam_role.gha_plan.id
  policy = data.aws_iam_policy_document.gha_plan_state_lock.json
}

# ------------------------------------------------------------------------------
# Deploy-time role: assumed by `main`-branch CD workflow runs only.
# ------------------------------------------------------------------------------

data "aws_iam_policy_document" "gha_deploy_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [data.aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # `main` branch pushes and workflow_dispatch runs targeting `main`.
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.repo_subject}:*"]
    }
  }
}

resource "aws_iam_role" "gha_deploy" {
  name               = "gha-deploy-production"
  assume_role_policy = data.aws_iam_policy_document.gha_deploy_trust.json
}

# Broad management of the resources Terraform owns. Scoped by tag/ARN where
# we can; intentionally region-pinned. Tighten further once the resource ARN
# patterns are known after the first apply.
data "aws_iam_policy_document" "gha_deploy_permissions" {
  # Regional services — pinned to var.aws_region.
  statement {
    sid    = "ManageRegionalResources"
    effect = "Allow"

    actions = [
      "lambda:*",
      "apigateway:*",
      "ec2:*",
      "rds:*",
      "s3:*",
      "logs:*",
      "secretsmanager:*",
      "dynamodb:*",
      "acm:DescribeCertificate",
      "acm:ListCertificates"
    ]

    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  # CloudFront is a global service — aws:RequestedRegion does not match
  # var.aws_region for these calls, so the regional condition above would deny
  # them. CloudFront is owned by the persistent stack
  # (infra/envs/production-persistent/) and invalidated by the CD workflow.
  statement {
    sid    = "ManageCloudFront"
    effect = "Allow"

    actions = [
      "cloudfront:CreateDistribution",
      "cloudfront:UpdateDistribution",
      "cloudfront:DeleteDistribution",
      "cloudfront:GetDistribution",
      "cloudfront:GetDistributionConfig",
      "cloudfront:ListDistributions",
      "cloudfront:TagResource",
      "cloudfront:UntagResource",
      "cloudfront:ListTagsForResource",
      "cloudfront:CreateOriginAccessControl",
      "cloudfront:GetOriginAccessControl",
      "cloudfront:GetOriginAccessControlConfig",
      "cloudfront:UpdateOriginAccessControl",
      "cloudfront:DeleteOriginAccessControl",
      "cloudfront:ListOriginAccessControls",
      "cloudfront:CreateInvalidation",
      "cloudfront:GetInvalidation",
      "cloudfront:ListInvalidations",
    ]

    resources = ["*"]
  }

  # IAM is a global service — aws:RequestedRegion does not match var.aws_region
  # for these calls, so the regional condition above would deny them.
  statement {
    sid    = "ManageIam"
    effect = "Allow"

    actions = [
      "iam:GetRole",
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:UpdateRole",
      "iam:PutRolePolicy",
      "iam:DeleteRolePolicy",
      "iam:AttachRolePolicy",
      "iam:DetachRolePolicy",
      "iam:PassRole",
      "iam:GetPolicy",
      "iam:CreatePolicy",
      "iam:DeletePolicy",
      "iam:ListRolePolicies",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:GetRolePolicy",
      "iam:TagRole",
      "iam:UntagRole"
    ]

    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "gha_deploy" {
  role   = aws_iam_role.gha_deploy.id
  policy = data.aws_iam_policy_document.gha_deploy_permissions.json
}
