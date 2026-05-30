output "cloudfront_distribution_id" {
  value = module.cloudfront.distribution_id
}

output "cloudfront_domain_name" {
  description = "Default *.cloudfront.net hostname — the public frontend URL."
  value       = module.cloudfront.domain_name
}

output "frontend_bucket_name" {
  value = module.frontend_bucket.bucket_name
}
