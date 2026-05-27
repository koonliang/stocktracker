variable "bucket_name" {
  description = "Globally-unique S3 bucket name."
  type        = string
}

variable "cloudfront_distribution_arn" {
  description = "ARN of the CloudFront distribution allowed to read this bucket via OAC."
  type        = string
  default     = null
}

variable "enable_oac_policy" {
  description = "Whether to attach an OAC bucket policy. Use a static boolean so Terraform can evaluate count at plan time."
  type        = bool
  default     = false
}
