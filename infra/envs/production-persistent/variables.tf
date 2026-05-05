variable "aws_region" {
  description = "AWS region for the S3 frontend bucket. CloudFront itself is global."
  type        = string
  default     = "ap-southeast-1"
}
