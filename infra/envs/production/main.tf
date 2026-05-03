provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

# The Cloudflare provider requires explicit configuration even when no
# resources reference it. The variable defaults to a placeholder so plan
# flows work without any env setup; real apply paths must pass
# TF_VAR_cloudflare_api_token (or set CLOUDFLARE_API_TOKEN and leave the var
# unset). Phase 7 (T042) will switch this to a Secrets Manager data source.
provider "cloudflare" {
  api_token = var.cloudflare_api_token
}

# ---------- Locals ----------

data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  name_prefix = "stocktracker-production"
  account_id  = data.aws_caller_identity.current.account_id

  # Two AZs, deterministic ordering so the subnet/AZ mapping is stable across
  # plans even if AWS reshuffles the response.
  azs = slice(sort(data.aws_availability_zones.available.names), 0, 2)

  enable_cloudflare = var.domain_name != "" && var.cloudflare_zone_id != ""

  common_tags = {
    Project     = "stocktracker"
    Environment = "production"
    ManagedBy   = "terraform"
  }
}

# ---------- Origin shared secret ----------
# Generated locally for v1; Phase 7 (T041/T042) will move this into Secrets
# Manager. Value is consumed by frontend_bucket (S3 policy) and cloudflare
# (transform rule) so both sides agree.

resource "random_password" "origin_shared_secret" {
  length  = 48
  special = false
}

# ---------- Modules ----------

module "network" {
  source             = "../../modules/network"
  name_prefix        = local.name_prefix
  availability_zones = local.azs
}

module "lambda_backend" {
  source             = "../../modules/lambda_backend"
  name_prefix        = "${local.name_prefix}-app"
  subnet_ids         = module.network.private_subnet_ids
  security_group_id  = module.network.lambda_security_group_id
  log_retention_days = 14

  provisioned_concurrent_executions = var.provisioned_concurrency

  environment_variables = {
    QUARKUS_PROFILE = "prod"
    AWS_REGION_NAME = var.aws_region
  }
}

module "api_gateway" {
  source               = "../../modules/api_gateway"
  name_prefix          = local.name_prefix
  lambda_function_name = module.lambda_backend.function_name
  lambda_alias_name    = module.lambda_backend.alias_name
  lambda_alias_arn     = module.lambda_backend.alias_arn
  log_retention_days   = 14

  cors_allow_origins = local.enable_cloudflare ? ["https://app.${var.domain_name}"] : []
  domain_name        = local.enable_cloudflare ? "api.${var.domain_name}" : ""
  certificate_arn    = local.enable_cloudflare ? var.acm_certificate_arn : ""
}

module "frontend_bucket" {
  source               = "../../modules/frontend_bucket"
  bucket_name          = "${local.name_prefix}-frontend-${local.account_id}-${var.aws_region}"
  origin_shared_secret = local.enable_cloudflare ? random_password.origin_shared_secret.result : ""
}

module "cloudflare" {
  count  = local.enable_cloudflare ? 1 : 0
  source = "../../modules/cloudflare"

  zone_id              = var.cloudflare_zone_id
  zone_name            = var.domain_name
  app_origin_hostname  = module.frontend_bucket.website_endpoint
  api_origin_hostname  = module.api_gateway.domain_target_domain_name
  origin_shared_secret = random_password.origin_shared_secret.result
}
