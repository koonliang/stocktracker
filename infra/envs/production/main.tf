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

  # RDS owns the DB password in Secrets Manager (FR-025a). The function reads it
  # via the Secrets Manager extension at startup — see DATASOURCE_PASSWORD_SECRET_ARN.
  datasource_password_secret_arn = module.rds_mysql.master_user_secret_arn
  secrets_extension_layer_arn    = var.secrets_extension_layer_arn

  environment_variables = {
    QUARKUS_PROFILE                 = "prod"
    AWS_REGION_NAME                 = var.aws_region
    QUARKUS_DATASOURCE_JDBC_URL     = "jdbc:mysql://${module.rds_mysql.address}:${module.rds_mysql.port}/${module.rds_mysql.db_name}"
    QUARKUS_DATASOURCE_USERNAME     = "stocktracker"
    DATASOURCE_PASSWORD_SECRET_ARN  = module.rds_mysql.master_user_secret_arn
    QUARKUS_FLYWAY_MIGRATE_AT_START = "false"
  }
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

  datasource_password_secret_arn = module.rds_mysql.master_user_secret_arn
  secrets_extension_layer_arn    = var.secrets_extension_layer_arn

  environment_variables = {
    QUARKUS_PROFILE                    = "migrate"
    QUARKUS_DATASOURCE_JDBC_URL        = "jdbc:mysql://${module.rds_mysql.address}:${module.rds_mysql.port}/${module.rds_mysql.db_name}"
    QUARKUS_DATASOURCE_USERNAME        = "stocktracker"
    DATASOURCE_PASSWORD_SECRET_ARN     = module.rds_mysql.master_user_secret_arn
    STOCKTRACKER_DEV_BOOTSTRAP_ENABLED = "false"
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
