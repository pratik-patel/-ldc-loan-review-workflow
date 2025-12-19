variable "environment" {
  description = "Environment name"
  type        = string
}

variable "lambda_log_group_name" {
  description = "Lambda CloudWatch log group name"
  type        = string
}

variable "step_functions_log_group_name" {
  description = "Step Functions CloudWatch log group name"
  type        = string
}

variable "log_retention_days" {
  description = "Log retention in days"
  type        = number
  default     = 30
}
