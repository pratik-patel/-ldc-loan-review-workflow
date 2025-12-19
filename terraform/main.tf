terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "LDC-Loan-Review"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# DynamoDB Table for state persistence
module "dynamodb" {
  source = "./modules/dynamodb"

  table_name           = var.dynamodb_table_name
  environment          = var.environment
  billing_mode         = var.dynamodb_billing_mode
  read_capacity        = var.dynamodb_read_capacity
  write_capacity       = var.dynamodb_write_capacity
  point_in_time_recovery_enabled = var.dynamodb_point_in_time_recovery
}

# IAM Roles and Policies
module "iam" {
  source = "./modules/iam"

  environment = var.environment
  dynamodb_table_arn = module.dynamodb.table_arn
  sqs_queue_arn = module.sqs.queue_arn
  sns_topic_arn = module.sns.topic_arn
}

# SQS Queue for reclass confirmations
module "sqs" {
  source = "./modules/sqs"

  queue_name           = var.sqs_queue_name
  environment          = var.environment
  message_retention    = var.sqs_message_retention
  visibility_timeout   = var.sqs_visibility_timeout
}

# SNS Topic for email notifications
module "sns" {
  source = "./modules/sns"

  topic_name   = var.sns_topic_name
  environment  = var.environment
  display_name = "LDC Loan Review Notifications"
}

# SES Configuration
module "ses" {
  source = "./modules/ses"

  sender_email = var.ses_sender_email
  environment  = var.environment
}

# Lambda Function
module "lambda" {
  source = "./modules/lambda"

  function_name = var.lambda_function_name
  environment   = var.environment
  
  handler       = "org.springframework.cloud.function.adapter.aws.FunctionInvoker"
  runtime       = "java21"
  timeout       = var.lambda_timeout
  memory_size   = var.lambda_memory_size
  
  code_path     = var.lambda_function_code_path
  
  iam_role_arn  = module.iam.lambda_role_arn
  
  environment_variables = {
    DYNAMODB_TABLE      = module.dynamodb.table_name
    SQS_QUEUE_URL       = module.sqs.queue_url
    SNS_TOPIC_ARN       = module.sns.topic_arn
    SES_SENDER_EMAIL    = var.ses_sender_email
    PARAMETER_STORE_PREFIX = "/ldc-workflow"
    SPRING_CLOUD_FUNCTION_DEFINITION = "loanReviewRouter"
    MAIN_CLASS = "com.ldc.workflow.LambdaApplication"
  }
}

# Step Functions State Machine
module "step_functions" {
  source = "./modules/step-functions"

  state_machine_name = var.step_functions_state_machine_name
  environment        = var.environment
  state_machine_role_arn = module.iam.step_functions_role_arn
  log_retention_days = var.cloudwatch_log_retention_days
  
  lambda_functions_ready = module.lambda.function_arn
  
  reclass_timer_seconds = 5 # Override for Dev/Test
}

# CloudWatch Logs
module "cloudwatch" {
  source = "./modules/cloudwatch"

  environment = var.environment
  
  lambda_log_group_name = "/aws/lambda/${var.lambda_function_name}"
  step_functions_log_group_name = "/aws/stepfunctions/${var.step_functions_state_machine_name}"
  
  log_retention_days = var.cloudwatch_log_retention_days
}

# Parameter Store Configuration
module "parameter_store" {
  source = "./modules/parameter-store"

  parameter_store_prefix = var.parameter_store_prefix
  environment            = var.environment
  
  # Timing
  reclass_timer_seconds                = var.reclass_timer_seconds
  review_type_assignment_timeout_seconds = var.review_type_assignment_timeout_seconds
  loan_decision_timeout_seconds        = var.loan_decision_timeout_seconds
  max_reclass_attempts                 = var.max_reclass_attempts
  
  # Email
  email_templates     = var.email_templates
  notification_emails = var.notification_emails
  
  # API & Integration
  api_endpoints = var.api_endpoints
  
  # Business Rules
  business_rules = var.business_rules
  
  # Feature Flags
  feature_flags = var.feature_flags
  
  # Logging
  logging = var.logging
}
