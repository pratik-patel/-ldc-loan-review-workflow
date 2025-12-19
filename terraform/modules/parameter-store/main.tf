terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Email Templates
resource "aws_ssm_parameter" "email_template_repurchase" {
  name  = "/ldc-workflow/email-templates/repurchase"
  type  = "String"
  value = var.email_templates["repurchase"].body

  tags = {
    Name        = "repurchase-email-template"
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "email_template_reclass_expired" {
  name  = "/ldc-workflow/email-templates/reclass-expired"
  type  = "String"
  value = var.email_templates["reclass_expired"].body

  tags = {
    Name        = "reclass-expired-email-template"
    Environment = var.environment
  }
}

# Notification Emails
resource "aws_ssm_parameter" "notification_email_repurchase" {
  name  = "/ldc-workflow/notifications/repurchase-email"
  type  = "String"
  value = var.notification_emails["repurchase"]

  tags = {
    Name        = "repurchase-notification-email"
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "notification_email_reclass" {
  name  = "/ldc-workflow/notifications/reclass-email"
  type  = "String"
  value = var.notification_emails["reclass"]

  tags = {
    Name        = "reclass-notification-email"
    Environment = var.environment
  }
}

# Outputs
output "email_template_repurchase_name" {
  value       = aws_ssm_parameter.email_template_repurchase.name
  description = "Parameter Store name for repurchase email template"
}

output "email_template_reclass_expired_name" {
  value       = aws_ssm_parameter.email_template_reclass_expired.name
  description = "Parameter Store name for reclass expired email template"
}
