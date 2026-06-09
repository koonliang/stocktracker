terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# VPC + 2 private subnets in distinct AZs. Optional single-AZ NAT instance
# provides low-cost outbound internet for dependencies that do not work over
# interface endpoints, notably Cognito hosted-UI user pools' JWKS endpoint.

data "aws_region" "current" {}

data "aws_ami" "nat" {
  count       = var.enable_nat_instance ? 1 : 0
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-kernel-6.1-arm64"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.name_prefix}-vpc"
  }
}

resource "aws_subnet" "private" {
  count             = length(var.private_subnet_cidrs)
  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.name_prefix}-private-${var.availability_zones[count.index]}"
    Tier = "private"
  }
}

resource "aws_subnet" "public" {
  count                   = var.enable_nat_instance ? length(var.public_subnet_cidrs) : 0
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.name_prefix}-public-${var.availability_zones[count.index]}"
    Tier = "public"
  }
}

resource "aws_internet_gateway" "this" {
  count  = var.enable_nat_instance ? 1 : 0
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.name_prefix}-igw"
  }
}

resource "aws_route_table" "public" {
  count  = var.enable_nat_instance ? 1 : 0
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this[0].id
  }

  tags = {
    Name = "${var.name_prefix}-public"
  }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public[0].id
}

resource "aws_eip" "nat" {
  count  = var.enable_nat_instance ? 1 : 0
  domain = "vpc"

  tags = {
    Name = "${var.name_prefix}-nat"
  }
}

resource "aws_security_group" "nat" {
  count       = var.enable_nat_instance ? 1 : 0
  name        = "${var.name_prefix}-nat-instance"
  description = "NAT instance: egress for private-subnet Lambdas."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "Forward private Lambda traffic"
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    security_groups = [aws_security_group.lambda.id]
  }

  egress {
    description = "Internet egress"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name_prefix}-nat-instance"
  }
}

resource "aws_instance" "nat" {
  count                       = var.enable_nat_instance ? 1 : 0
  ami                         = data.aws_ami.nat[0].id
  instance_type               = var.nat_instance_type
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.nat[0].id]
  associate_public_ip_address = true
  source_dest_check           = false

  user_data = <<-EOF
    #!/bin/bash
    set -euxo pipefail
    sysctl -w net.ipv4.ip_forward=1
    cat >/etc/sysctl.d/99-nat-instance.conf <<'SYSCTL'
    net.ipv4.ip_forward = 1
    SYSCTL
    cat >/etc/systemd/system/nat-instance.service <<'SERVICE'
    [Unit]
    Description=Configure NAT instance packet forwarding
    After=network-online.target
    Wants=network-online.target

    [Service]
    Type=oneshot
    ExecStart=/bin/sh -c '/usr/sbin/iptables -t nat -C POSTROUTING -o ens5 -j MASQUERADE || /usr/sbin/iptables -t nat -A POSTROUTING -o ens5 -j MASQUERADE'
    RemainAfterExit=yes

    [Install]
    WantedBy=multi-user.target
    SERVICE
    systemctl daemon-reload
    systemctl enable --now nat-instance.service
  EOF

  tags = {
    Name = "${var.name_prefix}-nat"
  }
}

resource "aws_eip_association" "nat" {
  count         = var.enable_nat_instance ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  instance_id   = aws_instance.nat[0].id

  depends_on = [aws_internet_gateway.this]
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  dynamic "route" {
    for_each = var.enable_nat_instance ? [1] : []
    content {
      cidr_block           = "0.0.0.0/0"
      network_interface_id = aws_instance.nat[0].primary_network_interface_id
    }
  }

  tags = {
    Name = "${var.name_prefix}-private"
  }
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# ---------- Security groups ----------

resource "aws_security_group" "lambda" {
  name        = "${var.name_prefix}-lambda"
  description = "Outbound-only SG for the application Lambda."
  vpc_id      = aws_vpc.this.id

  egress {
    description = "All egress (restricted in practice by VPC routing)."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.name_prefix}-lambda" }
}

resource "aws_security_group" "rds" {
  name        = "${var.name_prefix}-rds"
  description = "RDS MySQL: ingress only from the Lambda SG."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "MySQL from Lambda"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda.id]
  }

  tags = { Name = "${var.name_prefix}-rds" }
}

resource "aws_security_group" "vpc_endpoints" {
  name        = "${var.name_prefix}-vpc-endpoints"
  description = "TLS ingress to interface endpoints from the Lambda SG."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "HTTPS from Lambda"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda.id]
  }

  tags = { Name = "${var.name_prefix}-vpc-endpoints" }
}

# ---------- VPC interface endpoints ----------
# Secrets Manager and CloudWatch Logs stay on interface endpoints. When NAT
# instance egress is enabled, public HTTPS endpoints such as Cognito JWKS use
# the private default route.

resource "aws_vpc_endpoint" "secretsmanager" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${data.aws_region.current.name}.secretsmanager"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = { Name = "${var.name_prefix}-secretsmanager" }
}

resource "aws_vpc_endpoint" "logs" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${data.aws_region.current.name}.logs"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = { Name = "${var.name_prefix}-logs" }
}
