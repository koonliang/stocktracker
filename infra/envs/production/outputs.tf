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
  description = "Public URL the frontend should call. Custom domain when domain_name is set, AWS endpoint otherwise."
  value       = module.api_gateway.invoke_url
}

output "frontend_bucket_name" {
  value = module.frontend_bucket.bucket_name
}

output "frontend_website_endpoint" {
  value = module.frontend_bucket.website_endpoint
}

output "public_frontend_url" {
  description = "Public URL the user should visit. Custom domain when set, S3 website endpoint otherwise."
  value       = local.enable_cloudflare ? "https://app.${var.domain_name}" : "http://${module.frontend_bucket.website_endpoint}"
}

output "cloudflare_zone_id" {
  value = var.cloudflare_zone_id
}
