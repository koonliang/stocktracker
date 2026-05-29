variable "name_prefix" {
  description = "Function name and resource prefix (e.g., \"stocktracker-production-app\")."
  type        = string
}

variable "handler" {
  # QuarkusStreamHandler is the JVM managed-runtime entry point: it boots the
  # Quarkus application and dispatches events into the HTTP layer. Pointing at
  # the inner LambdaHttpHandler directly never boots Quarkus ("No virtual
  # channel available"). Matches Quarkus' generated sam.jvm.yaml.
  description = "Lambda handler. Default targets quarkus-amazon-lambda-http (JVM managed runtime)."
  type        = string
  default     = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
}

variable "memory_size" {
  description = "Lambda memory (MB). 1024 picked for JVM warm-up vs cost — see research R-003."
  type        = number
  default     = 1024
}

variable "timeout_seconds" {
  description = "Lambda timeout."
  type        = number
  default     = 30
}

variable "subnet_ids" {
  description = "Private subnet IDs the Lambda runs in."
  type        = list(string)
}

variable "security_group_id" {
  description = "Lambda security group ID (from the network module)."
  type        = string
}

variable "secrets_arn_pattern" {
  description = "ARN pattern (with wildcard) the execution role can `GetSecretValue` against."
  type        = string
  default     = "arn:aws:secretsmanager:*:*:secret:stocktracker/*"
}

variable "environment_variables" {
  description = "Plain (non-secret) Lambda env vars — DB host, region, etc."
  type        = map(string)
  default     = {}
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention for the function log group."
  type        = number
  default     = 14
}

variable "provisioned_concurrent_executions" {
  description = "Provisioned concurrency on the `production` alias. 0 disables it (default — see research R-003)."
  type        = number
  default     = 0
}
