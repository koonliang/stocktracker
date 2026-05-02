output "bucket_name" {
  value = aws_s3_bucket.this.bucket
}

output "bucket_arn" {
  value = aws_s3_bucket.this.arn
}

output "website_endpoint" {
  description = "Hostname of the S3 website endpoint — used as the Cloudflare CNAME target."
  value       = aws_s3_bucket_website_configuration.this.website_endpoint
}

output "regional_domain_name" {
  description = "Regional S3 bucket DNS name (path-style virtual host)."
  value       = aws_s3_bucket.this.bucket_regional_domain_name
}
