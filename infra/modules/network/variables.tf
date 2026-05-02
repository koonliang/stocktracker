variable "name_prefix" {
  description = "Prefix applied to resource names and tags (e.g., \"stocktracker-production\")."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "private_subnet_cidrs" {
  description = "Two private-subnet CIDRs, one per AZ in `availability_zones`."
  type        = list(string)
  default     = ["10.40.1.0/24", "10.40.2.0/24"]
  validation {
    condition     = length(var.private_subnet_cidrs) == 2
    error_message = "Exactly two private subnets are required."
  }
}

variable "availability_zones" {
  description = "Two AZs to place the private subnets into."
  type        = list(string)
  validation {
    condition     = length(var.availability_zones) == 2
    error_message = "Exactly two AZs are required."
  }
}
