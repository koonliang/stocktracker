provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

data "aws_caller_identity" "current" {}

locals {
  name_prefix = "stocktracker-production"
  account_id  = data.aws_caller_identity.current.account_id
  bucket_name = "${local.name_prefix}-frontend-${local.account_id}-${var.aws_region}"

  # Constructed deterministically to break the bucket ↔ distribution module
  # cycle (distribution needs origin domain; bucket policy needs distribution
  # ARN). The actual bucket resource still produces this exact value.
  bucket_regional_domain_name = "${local.bucket_name}.s3.${var.aws_region}.amazonaws.com"

  common_tags = {
    Project     = "stocktracker"
    Environment = "production"
    ManagedBy   = "terraform"
    Stack       = "persistent"
  }
}

module "frontend_bucket" {
  source                      = "../../modules/frontend_bucket"
  bucket_name                 = local.bucket_name
  cloudfront_distribution_arn = module.cloudfront.distribution_arn
}

module "cloudfront" {
  source                      = "../../modules/cloudfront"
  name_prefix                 = local.name_prefix
  bucket_name                 = local.bucket_name
  bucket_regional_domain_name = local.bucket_regional_domain_name
}
