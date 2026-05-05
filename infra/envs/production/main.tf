provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
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

  common_tags = {
    Project     = "stocktracker"
    Environment = "production"
    ManagedBy   = "terraform"
    Stack       = "ephemeral"
  }
}

# ---------- Persistent stack outputs ----------
# CloudFront distribution + frontend bucket live in envs/production-persistent
# and stay up between test sessions. We read them here so API Gateway CORS
# can allow the live CloudFront origin.

data "terraform_remote_state" "persistent" {
  backend = "s3"
  config = {
    bucket = "stocktracker-tfstate-309779120361-ap-southeast-1"
    key    = "envs/production/persistent.tfstate"
    region = "ap-southeast-1"
  }
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

  cors_allow_origins = ["https://${data.terraform_remote_state.persistent.outputs.cloudfront_domain_name}"]
}
