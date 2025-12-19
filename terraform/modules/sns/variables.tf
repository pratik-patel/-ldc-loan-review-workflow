variable "topic_name" {
  description = "SNS topic name"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "display_name" {
  description = "SNS topic display name"
  type        = string
  default     = "LDC Loan Review Notifications"
}
