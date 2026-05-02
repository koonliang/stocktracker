variable "bucket_name" {
  description = "Globally-unique S3 bucket name."
  type        = string
}

variable "origin_shared_secret" {
  description = "The Cloudflare-injected Referer value that gates `s3:GetObject`. Empty leaves the bucket without a public-read policy (private)."
  type        = string
  sensitive   = true
  default     = ""
}
