terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# SES Email Identity (for sending emails)
resource "aws_ses_email_identity" "sender" {
  email = var.sender_email
}

# Note: In production, you would also verify the email address
# This requires clicking a verification link sent to the email

output "sender_email" {
  value       = aws_ses_email_identity.sender.email
  description = "SES sender email"
}

output "sender_arn" {
  value       = aws_ses_email_identity.sender.arn
  description = "SES email identity ARN"
}
