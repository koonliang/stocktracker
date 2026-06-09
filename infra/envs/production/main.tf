provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

# ---------- Locals ----------

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  name_prefix = "stocktracker-production"

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

locals {
  # The persistent stack is applied first and left up; until it has been
  # applied its remote state carries no outputs. Guard the reads with try()
  # so `terraform plan` (including the CI plan job) does not hard-fail before
  # the persistent stack exists. The real values flow through once it is applied.
  persistent_outputs         = data.terraform_remote_state.persistent.outputs
  cloudfront_domain_name     = try(local.persistent_outputs.cloudfront_domain_name, "")
  cloudfront_distribution_id = try(local.persistent_outputs.cloudfront_distribution_id, "")
  frontend_bucket_name       = try(local.persistent_outputs.frontend_bucket_name, "")

  # No origin until the persistent stack supplies the CloudFront domain.
  cors_allow_origins = local.cloudfront_domain_name != "" ? ["https://${local.cloudfront_domain_name}"] : []

  # Hosted-UI redirect targets; only meaningful once the CloudFront origin exists.
  cognito_callback_urls = local.cloudfront_domain_name != "" ? ["https://${local.cloudfront_domain_name}/auth/callback"] : []
  cognito_logout_urls   = local.cloudfront_domain_name != "" ? ["https://${local.cloudfront_domain_name}/signed-out"] : []
}

# ---------- Modules ----------

module "network" {
  source              = "../../modules/network"
  name_prefix         = local.name_prefix
  availability_zones  = local.azs
  enable_nat_instance = true
  nat_instance_type   = "t4g.micro"
}

module "lambda_backend" {
  source             = "../../modules/lambda_backend"
  name_prefix        = "${local.name_prefix}-app"
  subnet_ids         = module.network.private_subnet_ids
  security_group_id  = module.network.lambda_security_group_id
  log_retention_days = 14

  provisioned_concurrent_executions = var.provisioned_concurrency

  # RDS owns the DB password in Secrets Manager (FR-025a). The function reads it
  # directly via the AWS SDK at startup — see DATASOURCE_PASSWORD_SECRET_ARN.
  datasource_password_secret_arn = module.rds_mysql.master_user_secret_arn

  environment_variables = {
    QUARKUS_PROFILE                 = "prod"
    AWS_REGION_NAME                 = var.aws_region
    QUARKUS_DATASOURCE_JDBC_URL     = "jdbc:mysql://${module.rds_mysql.address}:${module.rds_mysql.port}/${module.rds_mysql.db_name}"
    QUARKUS_DATASOURCE_USERNAME     = "stocktracker"
    DATASOURCE_PASSWORD_SECRET_ARN  = module.rds_mysql.master_user_secret_arn
    QUARKUS_FLYWAY_MIGRATE_AT_START = "false"
    # Reference data is seeded by the migrator, not the request-serving backend.
    STOCKTRACKER_DEV_BOOTSTRAP_ENABLED = "false"
    # Production delegates identity to Cognito; the backend only validates pool-issued
    # JWTs (contracts/cognito.md). The dev /api/auth/* + dev token endpoints go dark.
    STOCKTRACKER_AUTH_MODE = "cognito"
    COGNITO_ISSUER         = module.cognito.issuer
    COGNITO_JWKS_URL       = module.cognito.jwks_url
  }
}

module "cognito" {
  source        = "../../modules/cognito"
  name_prefix   = local.name_prefix
  domain_prefix = "${local.name_prefix}-auth"

  callback_urls = local.cognito_callback_urls
  logout_urls   = local.cognito_logout_urls

  # Google/Facebook credentials live in Secrets Manager; supply the secret names to
  # enable each provider (empty by default so a base apply/validate stays clean).
  google_secret_name   = var.cognito_google_secret_name
  facebook_secret_name = var.cognito_facebook_secret_name
}

module "rds_mysql" {
  source            = "../../modules/rds_mysql"
  name_prefix       = local.name_prefix
  subnet_ids        = module.network.private_subnet_ids
  security_group_id = module.network.rds_security_group_id
}

module "lambda_migrator" {
  source             = "../../modules/lambda_migrator"
  name_prefix        = "${local.name_prefix}-migrator"
  subnet_ids         = module.network.private_subnet_ids
  security_group_id  = module.network.lambda_security_group_id
  log_retention_days = 14

  # Seeding ~39k reference-data rows runs longer than the default 120s.
  timeout_seconds = 300

  datasource_password_secret_arn = module.rds_mysql.master_user_secret_arn

  environment_variables = {
    QUARKUS_PROFILE                = "migrate"
    QUARKUS_DATASOURCE_JDBC_URL    = "jdbc:mysql://${module.rds_mysql.address}:${module.rds_mysql.port}/${module.rds_mysql.db_name}"
    QUARKUS_DATASOURCE_USERNAME    = "stocktracker"
    DATASOURCE_PASSWORD_SECRET_ARN = module.rds_mysql.master_user_secret_arn
    # The migrator (not the backend) owns reference-data seeding.
    STOCKTRACKER_DEV_BOOTSTRAP_ENABLED = "true"
  }
}

module "api_gateway" {
  source               = "../../modules/api_gateway"
  name_prefix          = local.name_prefix
  lambda_function_name = module.lambda_backend.function_name
  lambda_alias_name    = module.lambda_backend.alias_name
  lambda_alias_arn     = module.lambda_backend.alias_arn
  log_retention_days   = 14

  cors_allow_origins = local.cors_allow_origins
}
