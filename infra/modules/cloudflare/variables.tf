variable "zone_id" {
  description = "Cloudflare zone ID for the apex domain."
  type        = string
}

variable "zone_name" {
  description = "Apex domain (e.g., example.com). Used to build the host filter in the transform rule."
  type        = string
}

variable "app_subdomain" {
  description = "Subdomain label for the frontend (e.g., \"app\" → app.example.com)."
  type        = string
  default     = "app"
}

variable "api_subdomain" {
  description = "Subdomain label for the API (e.g., \"api\" → api.example.com)."
  type        = string
  default     = "api"
}

variable "app_origin_hostname" {
  description = "Origin hostname for the `app` CNAME (S3 website endpoint)."
  type        = string
}

variable "api_origin_hostname" {
  description = "Origin hostname for the `api` CNAME (API Gateway custom-domain target). Empty disables the api record."
  type        = string
  default     = ""
}

variable "origin_shared_secret" {
  description = "Shared secret injected as `Referer` by the transform rule and matched by the S3 bucket policy."
  type        = string
  sensitive   = true
}
