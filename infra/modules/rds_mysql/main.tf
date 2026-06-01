terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-db"
  subnet_ids = var.subnet_ids

  tags = { Name = "${var.name_prefix}-db" }
}

resource "aws_db_parameter_group" "this" {
  name   = "${var.name_prefix}-mysql80"
  family = "mysql8.0"

  tags = { Name = "${var.name_prefix}-mysql80" }
}

resource "aws_db_instance" "this" {
  identifier = "${var.name_prefix}-mysql"

  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.master_username

  # RDS owns the master credential and stores it in Secrets Manager — no
  # plaintext password in Terraform state or variables (spec FR-025a). The
  # generated secret ARN is exposed via the master_user_secret_arn output for
  # the Lambda to read at runtime.
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  parameter_group_name   = aws_db_parameter_group.this.name
  vpc_security_group_ids = [var.security_group_id]

  publicly_accessible     = false
  multi_az                = false
  backup_retention_period = var.backup_retention_days
  skip_final_snapshot     = true

  tags = { Name = "${var.name_prefix}-mysql" }
}
