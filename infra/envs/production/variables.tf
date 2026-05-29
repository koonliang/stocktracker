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
  description = "Master password for the RDS MySQL instance (8-41 chars; no '/', '\"', '@', or spaces)."
  type        = string
  sensitive   = true

  validation {
    condition = (
      length(var.rds_master_password) >= 8 &&
      length(var.rds_master_password) <= 41 &&
      can(regex("^[^/@\" ]+$", var.rds_master_password))
    )
    error_message = "rds_master_password must be 8-41 characters and must not contain '/', '\"', '@', or spaces (RDS MySQL constraints). Check the RDS_MASTER_PASSWORD GitHub Actions secret."
  }
}
