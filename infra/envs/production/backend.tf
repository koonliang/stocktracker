terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Replace the placeholder bucket and dynamodb_table values with the outputs
  # from `infra/bootstrap` after its first apply. They are deliberately not
  # variables — Terraform requires literal values here.
  backend "s3" {
    bucket         = "stocktracker-tfstate-309779120361-ap-southeast-1"
    key            = "envs/production/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "stocktracker-tflock"
    encrypt        = true
  }
}
