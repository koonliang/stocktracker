output "state_bucket_name" {
  description = "S3 bucket holding Terraform remote state for all environments."
  value       = aws_s3_bucket.tfstate.bucket
}

output "lock_table_name" {
  description = "DynamoDB table used for Terraform state locking."
  value       = aws_dynamodb_table.tflock.name
}

output "gha_plan_role_arn" {
  description = "IAM role assumed by PR (plan-only) workflow runs. Set as repo variable AWS_PLAN_ROLE_ARN."
  value       = aws_iam_role.gha_plan.arn
}

output "gha_deploy_role_arn" {
  description = "IAM role assumed by main-branch (apply/deploy) workflow runs. Set as repo variable AWS_DEPLOY_ROLE_ARN."
  value       = aws_iam_role.gha_deploy.arn
}

output "github_oidc_provider_arn" {
  description = "ARN of the GitHub OIDC provider; consumed by trust policies."
  value       = data.aws_iam_openid_connect_provider.github.arn
}
