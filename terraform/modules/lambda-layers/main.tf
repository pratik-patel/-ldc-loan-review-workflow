terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Lambda Layer for Dependencies (Spring Boot, AWS SDK, etc.)
resource "aws_lambda_layer_version" "dependencies" {
  filename            = var.dependencies_layer_path
  layer_name          = "ldc-loan-review-dependencies"
  compatible_runtimes = ["java17"]
  source_code_hash    = filebase64sha256(var.dependencies_layer_path)

  depends_on = []

  lifecycle {
    create_before_destroy = true
  }
}

# Lambda Layer for Shared Code (Types, Utilities, Configuration)
resource "aws_lambda_layer_version" "shared" {
  filename            = var.shared_layer_path
  layer_name          = "ldc-loan-review-shared"
  compatible_runtimes = ["java17"]
  source_code_hash    = filebase64sha256(var.shared_layer_path)

  depends_on = []

  lifecycle {
    create_before_destroy = true
  }
}

# Output layer ARNs for use in Lambda function
output "dependencies_layer_arn" {
  value       = aws_lambda_layer_version.dependencies.arn
  description = "ARN of the dependencies Lambda layer"
}

output "shared_layer_arn" {
  value       = aws_lambda_layer_version.shared.arn
  description = "ARN of the shared code Lambda layer"
}

output "dependencies_layer_version" {
  value       = aws_lambda_layer_version.dependencies.version
  description = "Version of the dependencies Lambda layer"
}

output "shared_layer_version" {
  value       = aws_lambda_layer_version.shared.version
  description = "Version of the shared code Lambda layer"
}
