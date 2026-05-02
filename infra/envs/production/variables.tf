variable "aws_region" {
  description = "AWS region for all resources in this environment."
  type        = string
  default     = "ap-southeast-1"
}

variable "domain_name" {
  description = "Apex domain that hosts `app.<domain>` (frontend) and `api.<domain>` (backend). Empty disables the custom domain + Cloudflare wiring (see infra/README.md)."
  type        = string
  default     = ""
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN in the same region — required when domain_name is set."
  type        = string
  default     = ""
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID for `domain_name`. Required when domain_name is set."
  type        = string
  default     = ""
}

variable "provisioned_concurrency" {
  description = "Provisioned concurrency on the application Lambda's `production` alias. 0 disables it."
  type        = number
  default     = 0
}
