variable "name_prefix" {
  description = "Resource name prefix (e.g., \"stocktracker-production\")."
  type        = string
}

variable "domain_prefix" {
  description = "Hosted-UI domain prefix (must be globally unique within the region)."
  type        = string
}

variable "callback_urls" {
  description = "Allowed OAuth callback URLs (the CloudFront frontend origin + callback path)."
  type        = list(string)
  default     = []
}

variable "logout_urls" {
  description = "Allowed sign-out redirect URLs (the CloudFront frontend origin)."
  type        = list(string)
  default     = []
}

variable "password_minimum_length" {
  description = "Minimum password length; mirrors the dev-mode PasswordPolicy (FR-010)."
  type        = number
  default     = 8
}

variable "google_secret_name" {
  # The secret stores a JSON object: {\"client_id\":\"...\",\"client_secret\":\"...\"}.
  description = "Secrets Manager secret name holding the Google OAuth client credentials. Empty disables the Google IdP."
  type        = string
  default     = ""
}

variable "facebook_secret_name" {
  description = "Secrets Manager secret name holding the Facebook app credentials. Empty disables the Facebook IdP."
  type        = string
  default     = ""
}
