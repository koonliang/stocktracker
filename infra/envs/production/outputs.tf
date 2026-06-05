output "aws_region" {
  value = var.aws_region
}

output "lambda_function_name" {
  value = module.lambda_backend.function_name
}

output "lambda_alias_name" {
  value = module.lambda_backend.alias_name
}

output "lambda_role_arn" {
  value = module.lambda_backend.role_arn
}

output "api_id" {
  value = module.api_gateway.api_id
}

output "api_invoke_url" {
  description = "Public API URL — default API Gateway hostname (no custom domain in v1)."
  value       = module.api_gateway.invoke_url
}

output "cloudfront_domain_name" {
  description = "Public frontend URL — sourced from the persistent stack."
  value       = local.cloudfront_domain_name
}

output "cloudfront_distribution_id" {
  value = local.cloudfront_distribution_id
}

output "frontend_bucket_name" {
  value = local.frontend_bucket_name
}

output "rds_endpoint" {
  value = module.rds_mysql.endpoint
}

output "migrator_function_name" {
  value = module.lambda_migrator.function_name
}
