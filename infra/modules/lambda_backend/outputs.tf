output "function_name" {
  value = aws_lambda_function.this.function_name
}

output "function_arn" {
  value = aws_lambda_function.this.arn
}

output "alias_name" {
  value = aws_lambda_alias.production.name
}

output "alias_arn" {
  description = "Qualified ARN of the production alias — used as the API Gateway integration target."
  value       = aws_lambda_alias.production.arn
}

output "role_arn" {
  value = aws_iam_role.this.arn
}

output "role_name" {
  value = aws_iam_role.this.name
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.this.name
}
