variable "aws_region" {
  description = "AWS region for all resources in this environment."
  type        = string
  default     = "ap-southeast-1"
}

variable "provisioned_concurrency" {
  description = "Provisioned concurrency on the application Lambda's `production` alias. 0 disables it."
  type        = number
  default     = 0
}

variable "secrets_extension_layer_arn" {
  description = "ARN of the AWS Parameters and Secrets Lambda Extension layer (region/account-specific). Attached to the backend and migrator Lambdas so they resolve the RDS-managed DB password from the cached localhost endpoint."
  type        = string
  # ap-southeast-1, x86_64. See https://docs.aws.amazon.com/secretsmanager/latest/userguide/retrieving-secrets_lambda.html
  default = "arn:aws:lambda:ap-southeast-1:044395824272:layer:AWS-Parameters-and-Secrets-Lambda-Extension:11"
}
