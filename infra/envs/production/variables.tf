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

variable "cognito_google_secret_name" {
  description = "Secrets Manager secret name holding the Google OAuth client credentials. Empty disables Google federation."
  type        = string
  default     = ""
}

variable "cognito_facebook_secret_name" {
  description = "Secrets Manager secret name holding the Facebook app credentials. Empty disables Facebook federation."
  type        = string
  default     = ""
}
