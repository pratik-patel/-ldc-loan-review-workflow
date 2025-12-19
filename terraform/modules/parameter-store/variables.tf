variable "environment" {
  description = "Environment name"
  type        = string
}

variable "email_templates" {
  description = "Email templates for notifications"
  type = map(object({
    subject = string
    body    = string
  }))
}

variable "notification_emails" {
  description = "Email addresses for notifications"
  type        = map(string)
}
