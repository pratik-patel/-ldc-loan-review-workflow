terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# DynamoDB Table for Loan Review State
resource "aws_dynamodb_table" "loan_review_state" {
  name           = var.table_name
  billing_mode   = var.billing_mode
  hash_key       = "requestNumber"
  range_key      = "executionId"

  # Attributes
  attribute {
    name = "requestNumber"
    type = "S"
  }

  attribute {
    name = "executionId"
    type = "S"
  }

  attribute {
    name = "loanNumber"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  # Global Secondary Index for querying by loan number
  global_secondary_index {
    name            = "loanNumber-createdAt-index"
    hash_key        = "loanNumber"
    range_key       = "createdAt"
    projection_type = "ALL"
    read_capacity   = var.billing_mode == "PROVISIONED" ? var.read_capacity : null
    write_capacity  = var.billing_mode == "PROVISIONED" ? var.write_capacity : null
  }

  # Provisioned capacity (if not using on-demand)
  read_capacity  = var.billing_mode == "PROVISIONED" ? var.read_capacity : null
  write_capacity = var.billing_mode == "PROVISIONED" ? var.write_capacity : null

  # Point-in-time recovery
  point_in_time_recovery {
    enabled = var.point_in_time_recovery_enabled
  }

  # TTL for automatic cleanup (optional)
  ttl {
    attribute_name = "expirationTime"
    enabled        = true
  }

  # Encryption at rest
  server_side_encryption {
    enabled     = true
    kms_key_arn = null # Use AWS managed key
  }

  # Tags
  tags = {
    Name        = var.table_name
    Environment = var.environment
  }
}

# Outputs
output "table_name" {
  value       = aws_dynamodb_table.loan_review_state.name
  description = "DynamoDB table name"
}

output "table_arn" {
  value       = aws_dynamodb_table.loan_review_state.arn
  description = "DynamoDB table ARN"
}

output "table_id" {
  value       = aws_dynamodb_table.loan_review_state.id
  description = "DynamoDB table ID"
}

output "gsi_name" {
  value       = "loanNumber-createdAt-index"
  description = "Global Secondary Index name"
}
