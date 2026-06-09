# Amazon Cognito user pool that owns sign-up, verification, password reset, and
# Google/Facebook federation in production (contracts/cognito.md). The backend only
# validates pool-issued JWTs; verified-email account linking (FR-S03/S04) is performed
# backend-side in AccountLinkingService on first token, so no custom Cognito Lambda
# trigger is provisioned (out of scope per plan.md).

data "aws_region" "current" {}

resource "aws_cognito_user_pool" "this" {
  name = "${var.name_prefix}-users"

  # Email is the username/alias and is auto-verified by Cognito (account recovery via email).
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length    = var.password_minimum_length
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = false
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  schema {
    name                     = "email"
    attribute_data_type      = "String"
    required                 = true
    mutable                  = true
    developer_only_attribute = false

    string_attribute_constraints {
      min_length = 1
      max_length = 320
    }
  }
}

resource "aws_cognito_user_pool_domain" "this" {
  domain       = var.domain_prefix
  user_pool_id = aws_cognito_user_pool.this.id
}

# ---------- Federated identity providers ----------
# Credentials are read from Secrets Manager (consistent with the feature-003 secrets
# pattern). Each IdP is created only when its secret name is supplied, so a base
# deployment (or `terraform validate`/plan without secrets) stays clean.

data "aws_secretsmanager_secret_version" "google" {
  count     = var.google_secret_name != "" ? 1 : 0
  secret_id = var.google_secret_name
}

data "aws_secretsmanager_secret_version" "facebook" {
  count     = var.facebook_secret_name != "" ? 1 : 0
  secret_id = var.facebook_secret_name
}

resource "aws_cognito_identity_provider" "google" {
  count         = var.google_secret_name != "" ? 1 : 0
  user_pool_id  = aws_cognito_user_pool.this.id
  provider_name = "Google"
  provider_type = "Google"

  provider_details = {
    client_id        = jsondecode(data.aws_secretsmanager_secret_version.google[0].secret_string)["client_id"]
    client_secret    = jsondecode(data.aws_secretsmanager_secret_version.google[0].secret_string)["client_secret"]
    authorize_scopes = "openid email profile"
  }

  attribute_mapping = {
    email          = "email"
    email_verified = "email_verified"
    username       = "sub"
  }
}

resource "aws_cognito_identity_provider" "facebook" {
  count         = var.facebook_secret_name != "" ? 1 : 0
  user_pool_id  = aws_cognito_user_pool.this.id
  provider_name = "Facebook"
  provider_type = "Facebook"

  provider_details = {
    client_id        = jsondecode(data.aws_secretsmanager_secret_version.facebook[0].secret_string)["client_id"]
    client_secret    = jsondecode(data.aws_secretsmanager_secret_version.facebook[0].secret_string)["client_secret"]
    authorize_scopes = "email,public_profile"
    api_version      = "v17.0"
  }

  attribute_mapping = {
    email    = "email"
    username = "id"
  }
}

# ---------- App client (authorization-code flow) ----------

resource "aws_cognito_user_pool_client" "this" {
  name         = "${var.name_prefix}-web"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret = false

  allowed_oauth_flows                  = ["code"]
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_scopes                 = ["openid", "email", "profile"]

  callback_urls = var.callback_urls
  logout_urls   = var.logout_urls

  supported_identity_providers = concat(
    ["COGNITO"],
    var.google_secret_name != "" ? ["Google"] : [],
    var.facebook_secret_name != "" ? ["Facebook"] : [],
  )

  # Ensure the IdPs exist before the client references them.
  depends_on = [
    aws_cognito_identity_provider.google,
    aws_cognito_identity_provider.facebook,
  ]
}
