output "distribution_id" {
  value = aws_cloudfront_distribution.this.id
}

output "distribution_arn" {
  value = aws_cloudfront_distribution.this.arn
}

output "domain_name" {
  description = "Default *.cloudfront.net hostname for the distribution."
  value       = aws_cloudfront_distribution.this.domain_name
}
