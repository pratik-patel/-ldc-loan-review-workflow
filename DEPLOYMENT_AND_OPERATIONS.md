# LDC Loan Review Workflow - Deployment and Operations Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Deployment](#deployment)
5. [Configuration](#configuration)
6. [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)
7. [Operational Procedures](#operational-procedures)
8. [Rollback Procedures](#rollback-procedures)

---

## Overview

The LDC Loan Review Workflow is an AWS Step Functions-based orchestration system that automates the loan review process. It uses Lambda functions to handle various stages of the workflow, DynamoDB for state persistence, and integrates with external systems like Vend PPA and SES for email notifications.

### Key Components

- **AWS Step Functions**: Orchestrates the workflow state machine
- **AWS Lambda**: Executes business logic handlers
- **Amazon DynamoDB**: Persists workflow state
- **Amazon SQS**: Queues reclass confirmations
- **Amazon SNS**: Manages email notifications
- **Amazon SES**: Sends email notifications
- **AWS Systems Manager Parameter Store**: Stores configuration

---

## Architecture

### Workflow Flow

```
1. Review Type Validation
   ↓
2. Review Type Assignment (Pause for user input)
   ↓
3. Loan Decision (Pause for user input)
   ↓
4. Completion Criteria Check
   ↓
5. Loan Status Determination
   ↓
6. Decision Routing:
   - Approved/Denied → Vend PPA Integration
   - Repurchase → Email Notification
   - Reclass → 2-Day Timer → Email on Expiration
```

### Lambda Handlers

| Handler | Purpose | Trigger |
|---------|---------|---------|
| ReviewTypeValidationHandler | Validates review type | Step Functions |
| AttributeValidationHandler | Validates attribute decisions | Step Functions |
| CompletionCriteriaHandler | Checks completion criteria | Step Functions |
| LoanStatusDeterminationHandler | Determines final loan status | Step Functions |
| VendPpaIntegrationHandler | Integrates with Vend PPA | Step Functions |
| EmailNotificationHandler | Sends email notifications | Step Functions |
| SqsMessageHandler | Queues reclass confirmations | Step Functions |
| ReclassTimerExpirationHandler | Handles timer expiration | CloudWatch Events |
| ReviewTypeUpdateApiHandler | API to update review type | API Gateway |
| LoanDecisionUpdateApiHandler | API to update loan decision | API Gateway |
| AuditTrailHandler | Logs state transitions | Step Functions |

---

## Prerequisites

### Required Tools

- **Terraform** >= 1.0
- **AWS CLI** >= 2.0
- **Java** 21
- **Maven** >= 3.8.0
- **Git**

### AWS Permissions

Ensure your AWS credentials have permissions for:
- Lambda
- Step Functions
- DynamoDB
- SQS
- SNS
- SES
- Systems Manager Parameter Store
- CloudWatch
- IAM
- CloudFormation (for Terraform)

### AWS Account Setup

1. Create an AWS account or use existing account
2. Configure AWS CLI: `aws configure`
3. Verify credentials: `aws sts get-caller-identity`

---

## Deployment

### Step 1: Build the Lambda Function

```bash
cd ldc-loan-review-workflow
mvn clean package -DskipTests
```

This creates `lambda-function/target/lambda-function-1.0.0.jar`

### Step 2: Prepare Terraform Variables

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your environment-specific values
```

### Step 3: Initialize Terraform

```bash
terraform init
```

### Step 4: Plan Deployment

```bash
terraform plan -out=tfplan
```

Review the plan to ensure all resources will be created correctly.

### Step 5: Apply Terraform Configuration

```bash
terraform apply tfplan
```

This will:
- Create DynamoDB table
- Create SQS queue
- Create SNS topic
- Configure SES
- Deploy Lambda function
- Create Step Functions state machine
- Set up IAM roles and policies
- Configure CloudWatch logs

### Step 6: Verify Deployment

```bash
# Get Lambda function ARN
aws lambda get-function --function-name ldc-loan-review-lambda

# Get Step Functions state machine ARN
aws stepfunctions list-state-machines

# Test Lambda function
aws lambda invoke --function-name ldc-loan-review-lambda \
  --payload '{"handlerType":"reviewTypeValidation","requestNumber":"TEST-001","loanNumber":"LOAN-001","reviewType":"LDCReview"}' \
  response.json
cat response.json
```

---

## Configuration

### Environment Variables

Lambda function environment variables are set via Terraform:

```hcl
environment_variables = {
  DYNAMODB_TABLE      = "ldc-loan-review-state"
  SQS_QUEUE_URL       = "https://sqs.us-east-1.amazonaws.com/..."
  SNS_TOPIC_ARN       = "arn:aws:sns:us-east-1:..."
  SES_SENDER_EMAIL    = "noreply@ldc.com"
  PARAMETER_STORE_PREFIX = "/ldc-workflow"
}
```

### Parameter Store Configuration

Configuration values are stored in AWS Systems Manager Parameter Store:

```bash
# Email templates
aws ssm put-parameter --name "/ldc-workflow/email-templates/repurchase" \
  --value "Your loan has been marked for repurchase..." \
  --type String

# Notification emails
aws ssm put-parameter --name "/ldc-workflow/notification-emails/repurchase" \
  --value "repurchase-team@ldc.com" \
  --type String
```

### DynamoDB Table Schema

**Table Name**: `ldc-loan-review-state`

**Primary Key**: `requestNumber` (Partition Key) + `executionId` (Sort Key)

**Attributes**:
- `requestNumber`: Unique request identifier
- `executionId`: Step Functions execution ID
- `loanNumber`: Loan identifier
- `reviewType`: Type of review (LDCReview, SecPolicyReview, ConduitReview)
- `status`: Current workflow status
- `attributes`: List of loan attributes with decisions
- `loanDecision`: Final loan decision
- `currentAssignedUsername`: User assigned to review
- `taskToken`: Token for resuming paused workflows
- `createdAt`: Timestamp of creation
- `updatedAt`: Timestamp of last update

---

## Monitoring and Troubleshooting

### CloudWatch Logs

View Lambda function logs:

```bash
aws logs tail /aws/lambda/ldc-loan-review-lambda --follow
```

View Step Functions logs:

```bash
aws logs tail /aws/stepfunctions/ldc-loan-review-workflow --follow
```

### Common Issues

#### Lambda Function Timeout

**Symptom**: Step Functions execution fails with timeout error

**Solution**:
1. Increase Lambda timeout in `terraform.tfvars`: `lambda_timeout = 120`
2. Reapply Terraform: `terraform apply`
3. Check Lambda logs for performance issues

#### DynamoDB Throttling

**Symptom**: "ProvisionedThroughputExceededException"

**Solution**:
1. If using PROVISIONED mode, increase capacity in `terraform.tfvars`
2. Consider switching to PAY_PER_REQUEST mode for variable workloads

#### SES Email Not Sending

**Symptom**: Email notifications not received

**Solution**:
1. Verify SES sender email is verified: `aws ses verify-email-identity --email-address noreply@ldc.com`
2. Check SES sending limits: `aws ses get-account-sending-enabled`
3. Review CloudWatch logs for SES errors

### Debugging Workflow Execution

```bash
# Get execution history
aws stepfunctions get-execution-history \
  --state-machine-arn arn:aws:states:us-east-1:...:stateMachine:ldc-loan-review-workflow \
  --execution-arn arn:aws:states:us-east-1:...:execution:ldc-loan-review-workflow:...

# Describe execution
aws stepfunctions describe-execution \
  --execution-arn arn:aws:states:us-east-1:...:execution:ldc-loan-review-workflow:...
```

---

## Operational Procedures

### Starting a Workflow Execution

```bash
aws stepfunctions start-execution \
  --state-machine-arn arn:aws:states:us-east-1:...:stateMachine:ldc-loan-review-workflow \
  --input '{
    "requestNumber": "REQ-001",
    "loanNumber": "LOAN-001",
    "reviewType": "LDCReview"
  }'
```

### Resuming Paused Workflows

When a workflow is paused for user input (Review Type Assignment or Loan Decision), use the API endpoints to resume:

```bash
# Update review type and resume
curl -X POST https://api-gateway-url/review-type-update \
  -H "Content-Type: application/json" \
  -d '{
    "requestNumber": "REQ-001",
    "reviewType": "SecPolicyReview",
    "taskToken": "..."
  }'

# Update loan decision and resume
curl -X POST https://api-gateway-url/loan-decision-update \
  -H "Content-Type: application/json" \
  -d '{
    "requestNumber": "REQ-001",
    "loanDecision": "Approved",
    "taskToken": "..."
  }'
```

### Monitoring Workflow Progress

```bash
# List recent executions
aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:...:stateMachine:ldc-loan-review-workflow \
  --status-filter RUNNING

# Get execution details
aws stepfunctions describe-execution \
  --execution-arn arn:aws:states:us-east-1:...:execution:ldc-loan-review-workflow:execution-id
```

### Scaling Considerations

- **Lambda Concurrency**: Default is 1000 concurrent executions. Adjust via AWS Console if needed.
- **DynamoDB**: Use PAY_PER_REQUEST for variable workloads, PROVISIONED for predictable traffic.
- **SQS**: Automatically scales. Monitor queue depth in CloudWatch.

---

## Rollback Procedures

### Rollback to Previous Version

```bash
# Save current state
terraform state pull > terraform.state.backup

# Checkout previous version
git checkout <previous-commit>

# Reapply previous configuration
terraform apply

# Verify rollback
aws lambda get-function --function-name ldc-loan-review-lambda
```

### Emergency Rollback

If immediate rollback is needed:

```bash
# Disable Step Functions state machine
aws stepfunctions stop-execution \
  --execution-arn arn:aws:states:us-east-1:...:execution:...

# Disable Lambda function (set reserved concurrency to 0)
aws lambda put-function-concurrency \
  --function-name ldc-loan-review-lambda \
  --reserved-concurrent-executions 0

# Investigate issue
# ... fix issue ...

# Re-enable Lambda
aws lambda delete-function-concurrency \
  --function-name ldc-loan-review-lambda
```

### Data Recovery

DynamoDB point-in-time recovery is enabled by default. To restore:

```bash
# List available restore points
aws dynamodb describe-continuous-backups \
  --table-name ldc-loan-review-state

# Restore to point in time
aws dynamodb restore-table-to-point-in-time \
  --source-table-name ldc-loan-review-state \
  --target-table-name ldc-loan-review-state-restored \
  --restore-date-time 2024-01-15T10:00:00Z
```

---

## Support and Escalation

For issues or questions:

1. Check CloudWatch logs
2. Review this documentation
3. Contact the development team
4. Escalate to AWS support if needed

---

## Appendix: Useful Commands

```bash
# View all Lambda functions
aws lambda list-functions

# View all Step Functions state machines
aws stepfunctions list-state-machines

# View DynamoDB table details
aws dynamodb describe-table --table-name ldc-loan-review-state

# View SQS queue attributes
aws sqs get-queue-attributes --queue-url <queue-url> --attribute-names All

# View SNS topic attributes
aws sns get-topic-attributes --topic-arn <topic-arn>

# View CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=ldc-loan-review-lambda \
  --start-time 2024-01-15T00:00:00Z \
  --end-time 2024-01-16T00:00:00Z \
  --period 3600 \
  --statistics Average,Maximum
```

