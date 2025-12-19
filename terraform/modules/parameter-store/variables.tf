variable "parameter_store_prefix" {
  description = "Prefix for all Parameter Store parameters"
  type        = string
  default     = "ldc-workflow"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }
}

# Timing Parameters
variable "reclass_timer_seconds" {
  description = "Duration to wait before checking reclass confirmation (in seconds)"
  type        = number
  default     = 172800 # 48 hours
}

variable "review_type_assignment_timeout_seconds" {
  description = "Timeout for review type assignment stage (in seconds)"
  type        = number
  default     = 86400 # 24 hours
}

variable "loan_decision_timeout_seconds" {
  description = "Timeout for loan decision stage (in seconds)"
  type        = number
  default     = 604800 # 7 days
}

variable "max_reclass_attempts" {
  description = "Maximum number of reclass attempts allowed"
  type        = number
  default     = 3
}

# Email Configuration
variable "email_templates" {
  description = "Email templates for notifications"
  type = object({
    repurchase = object({
      subject = string
      body    = string
    })
    reclass_expired = object({
      subject = string
      body    = string
    })
    review_type_assignment = object({
      subject = string
      body    = string
    })
  })
  default = {
    repurchase = {
      subject = "Loan Repurchase Decision"
      body    = "Your loan has been marked for repurchase. Please review the details and take appropriate action."
    }
    reclass_expired = {
      subject = "Reclass Confirmation Expired"
      body    = "The reclass confirmation period has expired. The loan will proceed with the original decision."
    }
    review_type_assignment = {
      subject = "Review Type Assignment Required"
      body    = "A new loan review has been assigned. Please review and assign the appropriate review type."
    }
  }
}

variable "notification_emails" {
  description = "Email addresses for notifications"
  type = object({
    repurchase_team = string
    reclass_team    = string
    admin           = string
  })
  default = {
    repurchase_team = "repurchase@ldc.com"
    reclass_team    = "reclass@ldc.com"
    admin           = "admin@ldc.com"
  }
}

# API & Integration Endpoints
variable "api_endpoints" {
  description = "API endpoints and configuration"
  type = object({
    vend_ppa_endpoint        = string
    vend_ppa_timeout_seconds = number
    vend_ppa_retry_attempts  = number
  })
  default = {
    vend_ppa_endpoint        = "https://api.vendppa.com/v1/loans"
    vend_ppa_timeout_seconds = 30
    vend_ppa_retry_attempts  = 3
  }
}

# Business Rules
variable "business_rules" {
  description = "Business rules and validation thresholds"
  type = object({
    allowed_review_types        = list(string)
    allowed_attribute_decisions = list(string)
    credit_score_threshold      = string
    debt_ratio_threshold        = string
  })
  default = {
    allowed_review_types        = ["LDCReview", "SecPolicyReview", "ConduitReview"]
    allowed_attribute_decisions = ["Approved", "Rejected", "Reclass", "Repurchase", "Pending"]
    credit_score_threshold      = "620"
    debt_ratio_threshold        = "0.43"
  }
}

# Feature Flags
variable "feature_flags" {
  description = "Feature flags for enabling/disabling functionality"
  type = object({
    enable_vend_ppa_integration = string
    enable_email_notifications  = string
    enable_audit_logging        = string
    reclass_feature_enabled     = string
  })
  default = {
    enable_vend_ppa_integration = "true"
    enable_email_notifications  = "true"
    enable_audit_logging        = "true"
    reclass_feature_enabled     = "true"
  }
}

# Monitoring & Logging
variable "logging" {
  description = "Logging and monitoring configuration"
  type = object({
    log_level                    = string
    enable_detailed_logging      = string
    cloudwatch_metric_namespace  = string
  })
  default = {
    log_level                   = "INFO"
    enable_detailed_logging     = "false"
    cloudwatch_metric_namespace = "LDCLoanReview"
  }
}
