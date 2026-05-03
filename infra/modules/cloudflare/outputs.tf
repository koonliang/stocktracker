output "app_hostname" {
  value = "${var.app_subdomain}.${var.zone_name}"
}

output "api_hostname" {
  value = var.api_origin_hostname == "" ? "" : "${var.api_subdomain}.${var.zone_name}"
}
