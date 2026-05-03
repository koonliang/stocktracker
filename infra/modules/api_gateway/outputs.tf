output "api_id" {
  value = aws_apigatewayv2_api.this.id
}

output "api_endpoint" {
  description = "Auto-issued AWS invoke URL (always available)."
  value       = aws_apigatewayv2_api.this.api_endpoint
}

output "invoke_url" {
  description = "The URL the public should hit. Custom domain when enabled, AWS endpoint otherwise."
  value       = local.enable_custom_domain ? "https://${var.domain_name}" : aws_apigatewayv2_api.this.api_endpoint
}

output "domain_target_domain_name" {
  description = "Target hostname to point a Cloudflare CNAME at when a custom domain is enabled. Empty otherwise."
  value       = local.enable_custom_domain ? aws_apigatewayv2_domain_name.this[0].domain_name_configuration[0].target_domain_name : ""
}

output "stage_name" {
  value = aws_apigatewayv2_stage.default.name
}
