variable "name_prefix" {
  description = "Function name and resource prefix (e.g., \"stocktracker-production-migrator\")."
  type        = string
}

variable "handler" {
  # QuarkusStreamHandler boots Quarkus (running Flyway at startup via the
  # 'migrate' profile), then dispatches the invocation. Same entry point as the
  # backend. See lambda_backend for why the inner LambdaHttpHandler fails.
  description = "Lambda handler (JVM managed runtime). Boots Quarkus; Flyway runs at startup via the 'migrate' profile."
  type        = string
  default     = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
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

variable "datasource_password_secret_arn" {
  description = "ARN of the RDS-managed secret holding the DB master credential. Granted to the execution role and exposed as DATASOURCE_PASSWORD_SECRET_ARN."
  type        = string
  default     = ""
}

variable "secrets_extension_layer_arn" {
  description = "ARN of the AWS Parameters and Secrets Lambda Extension layer. Empty disables the layer."
  type        = string
  default     = ""
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
