variable "name_prefix" {
  description = "Name prefix used for the OAC and distribution comment."
  type        = string
}

variable "bucket_name" {
  description = "S3 bucket name (used to construct the origin id)."
  type        = string
}

variable "bucket_regional_domain_name" {
  description = "Regional domain name of the S3 bucket — the CloudFront origin."
  type        = string
}
