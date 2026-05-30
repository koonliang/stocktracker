output "bucket_name" {
  value = aws_s3_bucket.this.bucket
}

output "bucket_arn" {
  value = aws_s3_bucket.this.arn
}

output "bucket_id" {
  value = aws_s3_bucket.this.id
}

output "regional_domain_name" {
  description = "Regional S3 bucket DNS name — used as the CloudFront origin."
  value       = aws_s3_bucket.this.bucket_regional_domain_name
}
