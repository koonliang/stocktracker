terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Persistent stack — provisioned once and left up between test sessions.
  # Owns: CloudFront distribution + OAC + frontend S3 bucket.
  # The ephemeral stack (envs/production) reads this state via terraform_remote_state.
  backend "s3" {
    bucket         = "stocktracker-tfstate-309779120361-ap-southeast-1"
    key            = "envs/production/persistent.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "stocktracker-tflock"
    encrypt        = true
  }
}
