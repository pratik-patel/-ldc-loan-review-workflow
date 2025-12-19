variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }
}

# DynamoDB Configuration
variable "dynamodb_table_name" {
  description = "DynamoDB table name for state persistence"
  type        = string
  default     = "ldc-loan-review-state"
}

variable "dynamodb_billing_mode" {
  description = "DynamoDB billing mode (PAY_PER_REQUEST or PROVISIONED)"
  type        = string
  default     = "PAY_PER_REQUEST"
}

variable "dynamodb_read_capacity" {
  description = "DynamoDB read capacity units (only for PROVISIONED mode)"
  type        = number
  default     = 5
}

variable "dynamodb_write_capacity" {
  description = "DynamoDB write capacity units (only for PROVISIONED mode)"
  type        = number
  default     = 5
}

variable "dynamodb_point_in_time_recovery" {
  description = "Enable DynamoDB point-in-time recovery"
  type        = bool
  default     = true
}

# SQS Configuration
variable "sqs_queue_name" {
  description = "SQS queue name for reclass confirmations"
  type        = string
  default     = "ldc-loan-review-reclass-confirmations"
}

variable "sqs_message_retention" {
  description = "SQS message retention period in seconds"
  type        = number
  default     = 1209600 # 14 days
}

variable "sqs_visibility_timeout" {
  description = "SQS visibility timeout in seconds"
  type        = number
  default     = 300 # 5 minutes
}

# SNS Configuration
variable "sns_topic_name" {
  description = "SNS topic name for email notifications"
  type        = string
  default     = "ldc-loan-review-notifications"
}

# SES Configuration
variable "ses_sender_email" {
  description = "SES sender email address"
  type        = string
  default     = "noreply@ldc.com"
}

# Lambda Configuration
variable "lambda_function_name" {
  description = "Lambda function name"
  type        = string
  default     = "ldc-loan-review-lambda"
}

variable "lambda_timeout" {
  description = "Lambda function timeout in seconds"
  type        = number
  default     = 60
  validation {
    condition     = var.lambda_timeout >= 1 && var.lambda_timeout <= 900
    error_message = "Lambda timeout must be between 1 and 900 seconds."
  }
}

variable "lambda_memory_size" {
  description = "Lambda function memory size in MB"
  type        = number
  default     = 512
  validation {
    condition     = contains([128, 256, 512, 1024, 2048, 3008, 5120, 10240], var.lambda_memory_size)
    error_message = "Lambda memory size must be a valid value."
  }
}

variable "lambda_function_code_path" {
  description = "Path to Lambda function code JAR"
  type        = string
  default     = "../lambda-function/target/lambda-function-1.0.0.jar"
}

# Step Functions Configuration
variable "step_functions_state_machine_name" {
  description = "Step Functions state machine name"
  type        = string
  default     = "ldc-loan-review-workflow"
}

variable "step_functions_definition_path" {
  description = "Path to Step Functions state machine definition"
  type        = string
  default     = "./modules/step-functions/definition.asl.json"
}

# CloudWatch Configuration
variable "cloudwatch_log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 30
  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.cloudwatch_log_retention_days)
    error_message = "CloudWatch log retention must be a valid value."
  }
}

# Email Templates
variable "email_templates" {
  description = "Email templates for notifications"
  type = map(object({
    subject = string
    body    = string
  }))
  default = {
    repurchase = {
      subject = "Loan Repurchase Decision"
      body    = "Your loan has been marked for repurchase."
    }
    reclass_expired = {
      subject = "Reclass Confirmation Expired"
      body    = "The reclass confirmation period has expired."
    }
  }
}

# Notification Emails
variable "notification_emails" {
  description = "Email addresses for notifications"
  type = map(string)
  default = {
    repurchase = "repurchase@ldc.com"
    reclass    = "reclass@ldc.com"
  }
}
