variable "github_org" {
  description = "GitHub organisation or user that owns the repository."
  type        = string
  default     = "koonliang"
}

variable "github_repo" {
  description = "GitHub repository name."
  type        = string
  default     = "stocktracker"
}

variable "aws_region" {
  description = "AWS region for the environment this bootstrap supports."
  type        = string
  default     = "ap-southeast-1"
}
