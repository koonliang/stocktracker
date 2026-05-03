terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Private S3 bucket for the static frontend, served via the S3 website
# endpoint and reachable only through Cloudflare.
#
# Note on the origin-auth mechanism: S3 bucket policies cannot condition on
# arbitrary HTTP headers (e.g., a custom `X-Origin-Auth`). They can condition
# on `aws:Referer`, which is what we use here — the Cloudflare Transform Rule
# (cloudflare module) injects `Referer: <shared-secret>` on every request to
# the S3 origin. The behavioural intent matches research R-009: requests
# without the secret value get 403 from S3 directly.

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

resource "aws_s3_bucket_website_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  index_document {
    suffix = "index.html"
  }

  # SPA fallback: 404s in the bundle resolve back to index.html so client-side
  # routing handles them.
  error_document {
    key = "index.html"
  }
}

# Bucket policy guarded by `aws:Referer == <shared-secret>`. Only attached
# when a shared secret is supplied — without it the bucket stays private and
# is unreachable, which is the safe default during the very first apply
# before the secrets/Cloudflare wiring is in place.

data "aws_iam_policy_document" "origin_auth" {
  count = var.origin_shared_secret == "" ? 0 : 1

  statement {
    sid     = "AllowReadFromCloudflareOnly"
    effect  = "Allow"
    actions = ["s3:GetObject"]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    resources = ["${aws_s3_bucket.this.arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "aws:Referer"
      values   = [var.origin_shared_secret]
    }
  }
}

resource "aws_s3_bucket_policy" "origin_auth" {
  count  = var.origin_shared_secret == "" ? 0 : 1
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.origin_auth[0].json

  depends_on = [aws_s3_bucket_public_access_block.this]
}
