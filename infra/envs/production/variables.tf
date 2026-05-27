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

variable "rds_master_password" {
  description = "Master password for the RDS MySQL instance."
  type        = string
  sensitive   = true
}
