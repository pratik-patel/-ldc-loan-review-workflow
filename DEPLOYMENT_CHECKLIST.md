# LDC Loan Review Workflow - Deployment Checklist

## Pre-Deployment Verification

- [ ] AWS credentials configured: `aws sts get-caller-identity`
- [ ] AWS region set correctly: `aws configure get region`
- [ ] Terraform installed: `terraform version`
- [ ] Maven build successful: `mvn clean package -DskipTests`
- [ ] All tests passing: `mvn test`
- [ ] Git repository clean: `git status`

## Terraform Deployment Steps

### Step 1: Prepare Environment

```bash
cd ldc-loan-review-workflow/terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your environment-specific values
```

- [ ] terraform.tfvars created and configured
- [ ] AWS region verified in terraform.tfvars
- [ ] Environment name set (dev/staging/prod)

### Step 2: Initialize Terraform

```bash
terraform init
```

- [ ] Terraform initialized successfully
- [ ] .terraform directory created
- [ ] Backend configured

### Step 3: Plan Deployment

```bash
terraform plan -out=tfplan
```

- [ ] Plan generated successfully
- [ ] Review resource creation plan
- [ ] Verify no unexpected changes
- [ ] Save plan to tfplan file

### Step 4: Apply Configuration

```bash
terraform apply tfplan
```

- [ ] Terraform apply completed successfully
- [ ] All resources created
- [ ] No errors or warnings

## AWS Resource Verification

### Lambda Function

```bash
aws lambda get-function --function-name ldc-loan-review-lambda
```

- [ ] Lambda function created
- [ ] Function ARN noted: `_________________`
- [ ] Runtime is java21
- [ ] Handler is org.springframework.cloud.function.adapter.aws.FunctionInvoker
- [ ] Memory size is 512 MB
- [ ] Timeout is 60 seconds

### Step Functions State Machine

```bash
aws stepfunctions list-state-machines
aws stepfunctions describe-state-machine --state-machine-arn <arn>
```

- [ ] State machine created
- [ ] State machine ARN noted: `_________________`
- [ ] Definition is valid
- [ ] Role ARN is correct

### DynamoDB Table

```bash
aws dynamodb describe-table --table-name ldc-loan-review-state
```

- [ ] Table created
- [ ] Partition key: requestNumber
- [ ] Sort key: executionId
- [ ] Billing mode: PAY_PER_REQUEST
- [ ] Point-in-time recovery enabled

### SQS Queue

```bash
aws sqs list-queues
aws sqs get-queue-attributes --queue-url <url> --attribute-names All
```

- [ ] Queue created
- [ ] Queue URL noted: `_________________`
- [ ] Message retention: 14 days
- [ ] Visibility timeout: 5 minutes

### SNS Topic

```bash
aws sns list-topics
aws sns get-topic-attributes --topic-arn <arn>
```

- [ ] Topic created
- [ ] Topic ARN noted: `_________________`
- [ ] Display name set

### SES Configuration

```bash
aws ses list-verified-email-addresses
aws ses get-account-sending-enabled
```

- [ ] Sender email verified
- [ ] Sending enabled
- [ ] Email address: `_________________`

### CloudWatch Logs

```bash
aws logs describe-log-groups
```

- [ ] Lambda log group created: `/aws/lambda/ldc-loan-review-lambda`
- [ ] Step Functions log group created: `/aws/stepfunctions/ldc-loan-review-workflow`
- [ ] Log retention: 30 days

## Lambda Function Testing

### Test 1: Basic Handler Invocation

```bash
aws lambda invoke --function-name ldc-loan-review-lambda \
  --payload '{"handlerType":"reviewTypeValidation","requestNumber":"TEST-001","loanNumber":"LOAN-001","reviewType":"LDCReview"}' \
  response.json
cat response.json
```

- [ ] Lambda invoked successfully
- [ ] Response contains success: true
- [ ] Response contains requestNumber: TEST-001

### Test 2: Error Handling

```bash
aws lambda invoke --function-name ldc-loan-review-lambda \
  --payload '{"handlerType":"reviewTypeValidation","requestNumber":"TEST-002","loanNumber":"LOAN-002","reviewType":"InvalidType"}' \
  response.json
cat response.json
```

- [ ] Lambda invoked successfully
- [ ] Response contains success: false
- [ ] Response contains error message

## Step Functions Execution Testing

### Test 1: Start Execution

```bash
aws stepfunctions start-execution \
  --state-machine-arn <state-machine-arn> \
  --input '{"requestNumber":"TEST-001","loanNumber":"LOAN-001","reviewType":"LDCReview"}'
```

- [ ] Execution started successfully
- [ ] Execution ARN noted: `_________________`

### Test 2: Monitor Execution

```bash
aws stepfunctions describe-execution --execution-arn <execution-arn>
aws stepfunctions get-execution-history --execution-arn <execution-arn>
```

- [ ] Execution status checked
- [ ] Execution history reviewed
- [ ] No errors in execution

## DynamoDB State Verification

```bash
aws dynamodb get-item \
  --table-name ldc-loan-review-state \
  --key '{"requestNumber":{"S":"TEST-001"},"executionId":{"S":"<execution-id>"}}'
```

- [ ] State record created
- [ ] All required fields present
- [ ] Data is consistent

## Email Notification Testing

- [ ] Check email inbox for test notifications
- [ ] Verify email contains correct information
- [ ] Verify sender email is correct

## SQS Queue Testing

```bash
aws sqs receive-message --queue-url <queue-url>
```

- [ ] Messages received from queue
- [ ] Message format is correct
- [ ] Message contains expected data

## CloudWatch Monitoring

```bash
aws logs tail /aws/lambda/ldc-loan-review-lambda --follow
```

- [ ] Lambda logs are being written
- [ ] Log entries are readable
- [ ] No error messages in logs

## Post-Deployment Documentation

- [ ] AWS resource identifiers documented
- [ ] Endpoint URLs documented
- [ ] Configuration parameters documented
- [ ] Deployment date and time recorded: `_________________`
- [ ] Deployed by: `_________________`
- [ ] Deployment notes: `_________________`

## Rollback Preparation

- [ ] Terraform state backed up
- [ ] Previous version tagged in Git
- [ ] Rollback procedure tested
- [ ] Rollback contacts identified

## Sign-Off

- [ ] All checks passed
- [ ] Deployment verified
- [ ] Ready for production use

**Deployment Date**: `_________________`
**Deployed By**: `_________________`
**Approved By**: `_________________`

