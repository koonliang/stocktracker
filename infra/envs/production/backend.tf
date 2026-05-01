terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Replace the placeholder bucket and dynamodb_table values with the outputs
  # from `infra/bootstrap` after its first apply. They are deliberately not
  # variables — Terraform requires literal values here.
  backend "s3" {
    bucket         = "REPLACE_WITH_BOOTSTRAP_STATE_BUCKET"
    key            = "envs/production/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "stocktracker-tflock"
    encrypt        = true
  }
}
