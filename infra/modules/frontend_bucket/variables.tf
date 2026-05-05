variable "bucket_name" {
  description = "Globally-unique S3 bucket name."
  type        = string
}

variable "cloudfront_distribution_arn" {
  description = "ARN of the CloudFront distribution allowed to read this bucket via OAC. Empty leaves the bucket private with no policy attached (safe default)."
  type        = string
  default     = ""
}
