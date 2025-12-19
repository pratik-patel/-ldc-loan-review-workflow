terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

resource "aws_sns_topic" "notifications" {
  name              = var.topic_name
  display_name      = var.display_name
  kms_master_key_id = "alias/aws/sns"

  tags = {
    Name        = var.topic_name
    Environment = var.environment
  }
}

output "topic_arn" {
  value       = aws_sns_topic.notifications.arn
  description = "SNS topic ARN"
}
