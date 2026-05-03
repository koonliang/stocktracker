variable "name_prefix" {
  description = "Resource name prefix (e.g., \"stocktracker-production\")."
  type        = string
}

variable "lambda_function_name" {
  description = "Name of the application Lambda function (the alias is bound below)."
  type        = string
}

variable "lambda_alias_name" {
  description = "Lambda alias to invoke (e.g., \"production\")."
  type        = string
  default     = "production"
}

variable "lambda_alias_arn" {
  description = "ARN of the Lambda alias — used as the AWS_PROXY integration_uri."
  type        = string
}

variable "cors_allow_origins" {
  description = "Allowed origins for CORS. Default: empty list — supply the public frontend host."
  type        = list(string)
  default     = []
}

variable "throttling_burst_limit" {
  description = "Stage-level burst limit (per HTTP API contract)."
  type        = number
  default     = 200
}

variable "throttling_rate_limit" {
  description = "Stage-level rate limit, requests/sec (per HTTP API contract)."
  type        = number
  default     = 100
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention for the access-log group."
  type        = number
  default     = 14
}

variable "domain_name" {
  description = "Custom domain (e.g., api.example.com). Empty disables the custom domain."
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "ACM certificate ARN in the same region. Required when domain_name is set."
  type        = string
  default     = ""
}
