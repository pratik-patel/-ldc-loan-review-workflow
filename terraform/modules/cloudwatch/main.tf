terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# CloudWatch log groups are created automatically by Lambda and Step Functions
# No need to create them explicitly

# Outputs
output "lambda_log_group_name" {
  value       = var.lambda_log_group_name
  description = "Lambda CloudWatch log group name"
}

output "step_functions_log_group_name" {
  value       = var.step_functions_log_group_name
  description = "Step Functions CloudWatch log group name"
}
