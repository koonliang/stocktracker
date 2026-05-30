terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Private S3 bucket for the static frontend, served as the CloudFront origin
# via Origin Access Control (OAC). Direct public access is denied; only
# CloudFront — identified by the distribution ARN in the bucket policy — can
# read objects.

resource "aws_s3_bucket" "this" {
  bucket        = var.bucket_name
  force_destroy = true
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = aws_s3_bucket.this.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket                  = aws_s3_bucket.this.id
  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true
}

# OAC bucket policy — only attached when a CloudFront distribution ARN is
# provided. SPA fallback (404 → /index.html) is handled by CloudFront custom
# error responses, not S3 website routing, so no website configuration is
# created.

data "aws_iam_policy_document" "oac" {
  count = var.enable_oac_policy ? 1 : 0

  statement {
    sid     = "AllowCloudFrontReadOAC"
    effect  = "Allow"
    actions = ["s3:GetObject"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    resources = ["${aws_s3_bucket.this.arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [var.cloudfront_distribution_arn]
    }
  }
}

resource "aws_s3_bucket_policy" "oac" {
  count  = var.enable_oac_policy ? 1 : 0
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.oac[0].json

  depends_on = [aws_s3_bucket_public_access_block.this]
}
