variable "queue_name" {
  description = "SQS queue name"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "message_retention" {
  description = "Message retention period in seconds"
  type        = number
  default     = 1209600 # 14 days
}

variable "visibility_timeout" {
  description = "Visibility timeout in seconds"
  type        = number
  default     = 300 # 5 minutes
}
