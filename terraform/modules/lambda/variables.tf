variable "function_name" {
  description = "Lambda function name"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "handler" {
  description = "Lambda handler"
  type        = string
  default     = "org.springframework.cloud.function.adapter.aws.FunctionInvoker"
}

variable "runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java17"
}

variable "timeout" {
  description = "Lambda timeout in seconds"
  type        = number
  default     = 60
}

variable "memory_size" {
  description = "Lambda memory size in MB"
  type        = number
  default     = 512
}

variable "code_path" {
  description = "Path to Lambda function code JAR"
  type        = string
}

variable "iam_role_arn" {
  description = "IAM role ARN for Lambda function"
  type        = string
}

variable "layer_arns" {
  description = "List of Lambda layer ARNs"
  type        = list(string)
  default     = []
}

variable "environment_variables" {
  description = "Environment variables for Lambda function"
  type        = map(string)
  default     = {}
}
