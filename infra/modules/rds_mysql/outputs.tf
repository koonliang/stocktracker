output "endpoint" {
  description = "RDS endpoint (host:port)."
  value       = aws_db_instance.this.endpoint
}

output "address" {
  description = "RDS hostname (without port)."
  value       = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "db_name" {
  value = aws_db_instance.this.db_name
}

output "identifier" {
  value = aws_db_instance.this.identifier
}

output "master_user_secret_arn" {
  description = "ARN of the RDS-managed master credential secret in Secrets Manager (consumed by the Lambda at runtime)."
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
