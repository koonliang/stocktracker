variable "name_prefix" {
  description = "Function name and resource prefix (e.g., \"stocktracker-production-migrator\")."
  type        = string
}

variable "handler" {
  description = "Lambda handler. Same Quarkus HTTP handler as the backend; Flyway runs at startup via the 'migrate' profile."
  type        = string
  default     = "io.quarkus.amazon.lambda.http.LambdaHttpHandler::handleRequest"
}

variable "memory_size" {
  description = "Lambda memory (MB)."
  type        = number
  default     = 1024
}

variable "timeout_seconds" {
  description = "Lambda timeout — migrations may take longer than normal requests."
  type        = number
  default     = 120
}

variable "subnet_ids" {
  description = "Private subnet IDs the Lambda runs in (must reach RDS)."
  type        = list(string)
}

variable "security_group_id" {
  description = "Lambda security group ID (from the network module)."
  type        = string
}

variable "secrets_arn_pattern" {
  description = "ARN pattern the execution role can GetSecretValue against."
  type        = string
  default     = "arn:aws:secretsmanager:*:*:secret:stocktracker/*"
}

variable "environment_variables" {
  description = "Lambda env vars — DB host, credentials, Flyway config."
  type        = map(string)
  default     = {}
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention for the migrator log group."
  type        = number
  default     = 14
}
