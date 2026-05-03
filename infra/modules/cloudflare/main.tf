terraform {
  required_version = ">= 1.7.0"
  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
}

# DNS:
# - app.<domain>  → S3 website endpoint (proxied: orange-cloud, edge cache + WAF)
# - api.<domain>  → API Gateway custom-domain target (DNS-only: grey-cloud,
#                   AWS handles TLS at the regional endpoint)

resource "cloudflare_record" "app" {
  zone_id = var.zone_id
  name    = var.app_subdomain
  type    = "CNAME"
  value   = var.app_origin_hostname
  proxied = true
  ttl     = 1
  comment = "Frontend — proxied through Cloudflare to private S3 origin."
}

resource "cloudflare_record" "api" {
  count   = var.api_origin_hostname == "" ? 0 : 1
  zone_id = var.zone_id
  name    = var.api_subdomain
  type    = "CNAME"
  value   = var.api_origin_hostname
  proxied = false
  ttl     = 300
  comment = "API — DNS-only CNAME to API Gateway custom domain."
}

# Transform Rule — inject the shared secret as Referer on every request to the
# `app` host. The S3 bucket policy in frontend_bucket allows GetObject only
# when this header is present and matches.

resource "cloudflare_ruleset" "origin_auth" {
  zone_id = var.zone_id
  name    = "stocktracker-origin-auth"
  kind    = "zone"
  phase   = "http_request_late_transform"

  rules {
    action     = "rewrite"
    expression = "(http.host eq \"${var.app_subdomain}.${var.zone_name}\")"
    enabled    = true

    action_parameters {
      headers {
        name      = "Referer"
        operation = "set"
        value     = var.origin_shared_secret
      }
    }
  }
}
