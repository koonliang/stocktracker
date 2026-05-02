terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}

# Placeholder zip — the real artifact is uploaded by the CD workflow's
# `backend-deploy` step (`aws lambda update-function-code`). We `ignore_changes`
# on the package fields so subsequent `terraform apply` runs do not roll the
# code back to the placeholder.
data "archive_file" "placeholder" {
  type        = "zip"
  output_path = "${path.module}/.placeholder.zip"
  source {
    content  = "placeholder — real code uploaded by cd.yml"
    filename = "placeholder.txt"
  }
}

# ---------- IAM ----------

data "aws_iam_policy_document" "assume_lambda" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity", "sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name               = "${var.name_prefix}-exec"
  assume_role_policy = data.aws_iam_policy_document.assume_lambda.json
}

# CloudWatch Logs + ENI management for VPC-attached Lambdas. Managed policy.
resource "aws_iam_role_policy_attachment" "vpc_access" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

# Read access to the project's namespaced secrets only.
data "aws_iam_policy_document" "secrets_read" {
  statement {
    sid       = "ReadProjectSecrets"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
    resources = [var.secrets_arn_pattern]
  }
}

resource "aws_iam_role_policy" "secrets_read" {
  name   = "secrets-read"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.secrets_read.json
}

# ---------- Log group ----------

resource "aws_cloudwatch_log_group" "this" {
  name              = "/aws/lambda/${var.name_prefix}"
  retention_in_days = var.log_retention_days
}

# ---------- Function ----------

resource "aws_lambda_function" "this" {
  function_name = var.name_prefix
  role          = aws_iam_role.this.arn
  runtime       = "java21"
  handler       = var.handler
  memory_size   = var.memory_size
  timeout       = var.timeout_seconds
  architectures = ["x86_64"]
  publish       = true

  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [var.security_group_id]
  }

  environment {
    variables = var.environment_variables
  }

  depends_on = [
    aws_iam_role_policy_attachment.vpc_access,
    aws_cloudwatch_log_group.this,
  ]

  lifecycle {
    ignore_changes = [
      filename,
      source_code_hash,
      # The CD workflow publishes new versions and updates the alias; do not
      # let `terraform apply` revert the alias to the latest published version.
      qualified_arn,
    ]
  }
}

# ---------- Production alias ----------
# Initially points at $LATEST; the CD `backend-deploy` step swings it to the
# version it just published. We ignore alias function_version so terraform
# does not flip it back on every apply.

resource "aws_lambda_alias" "production" {
  name             = "production"
  function_name    = aws_lambda_function.this.function_name
  function_version = "$LATEST"

  lifecycle {
    ignore_changes = [function_version]
  }
}

# Optional provisioned concurrency on the production alias.
resource "aws_lambda_provisioned_concurrency_config" "production" {
  count                             = var.provisioned_concurrent_executions > 0 ? 1 : 0
  function_name                     = aws_lambda_function.this.function_name
  qualifier                         = aws_lambda_alias.production.name
  provisioned_concurrent_executions = var.provisioned_concurrent_executions
}
