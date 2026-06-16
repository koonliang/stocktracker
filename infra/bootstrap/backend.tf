terraform {
  # Bootstrap starts with local state only for the very first account setup.
  # After the state bucket and lock table exist, migrate this stack here with:
  #
  #   terraform init -migrate-state
  #
  # Terraform backend values must be literals, so keep these aligned with the
  # bootstrap outputs for the target AWS account and region.
  backend "s3" {
    bucket         = "stocktracker-tfstate-309779120361-ap-southeast-1"
    key            = "bootstrap/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "stocktracker-tflock"
    encrypt        = true
  }
}
