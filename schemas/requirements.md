# Requirements Document: LDC Loan Review Workflow

## Introduction

The LDC Loan Review Workflow is an AWS Step Functions-based system that orchestrates the loan decision review process. The workflow manages the complete lifecycle of loan reviews, from initial review type assignment through final decision routing (Vend PPA, Repurchase notification, or Reclass confirmation). The system handles multiple review types (LDC Review, Security Policy Review, Conduit Review), tracks attribute-level decisions, and implements state-driven transitions based on completion criteria and decision outcomes.

## Glossary

- **Step Function**: AWS Step Functions state machine that orchestrates the workflow
- **Review Type**: Classification of the review process (LDCReview, SecPolicyReview, ConduitReview)
- **Loan Decision**: Final determination on the loan (Approved, Denied, Partially Approved, Reclass Approved, Repurchase)
- **Attribute Decision**: Individual decision on a loan attribute (Approved, Rejected, Reclass, Repurchase, Pending)
- **Loan Decision Complete**: State where all attribute decisions are non-null and non-Pending, and loan decision is non-null
- **Vend PPA**: External API that records the final loan decision in the Business Engine
- **Reclass Confirmation**: Message received indicating completion of loan reclassification
- **IAG Groups**: Identity and Access Management groups for authorization
- **SQS**: Amazon Simple Queue Service for asynchronous message processing
- **State Machine**: The Step Functions workflow definition and execution context
- **startPPAreview**: API operation that initiates a new PPA review workflow instance
- **loan-ppa-request.schema.json**: JSON Schema defining the request contract for startPPAreview operation
- **loan-ppa-workflow-response.schema.json**: JSON Schema defining the response contract for startPPAreview operation
- **PostgreSQL Database**: Relational database storing loan and attribute information
- **loan_attributes Table**: PostgreSQL table containing attribute-level decisions and statuses for loans
- **Attribute Status Values**: Enumerated values for attribute decisions: Pending, Reclass, Approved, Rejected, Repurchase

## API Schemas

### Request Schema: loan-ppa-request.schema.json

The startPPAreview operation accepts a request payload conforming to the following schema defined in schemas/loan-ppa-request.schema.json

And response schemas/loan-ppa-response.schema.json 

Please note that the same schema would be used at mulitple places and hence the schema objects must be reused across multiple apis. 

## Requirements

### Requirement 1

**User Story:** As a loan review system, I want to validate and store the review type, so that the workflow can route to the appropriate review process.

#### Acceptance Criteria

1. WHEN the step function receives a JSON payload with a reviewType field THEN the system SHALL validate that reviewType is one of 'LDCReview', 'SecPolicyReview', or 'ConduitReview'
2. IF the reviewType value is outside the allowed values THEN the system SHALL return an error and allow reassignment of the reviewType
3. WHEN a valid reviewType is provided THEN the system SHALL store the reviewType in the state machine context
4. WHEN the reviewType is updated via API call THEN the system SHALL validate the new value against allowed values and update the state machine context

### Requirement 2

**User Story:** As a loan review system, I want to pause execution for human review type assignment, so that authorized users can determine the appropriate review classification.

#### Acceptance Criteria

1. WHEN the step function initializes with a reviewType THEN the system SHALL pause execution and wait for human input
2. WHEN a human updates the reviewType through an API call THEN the system SHALL resume execution with the updated reviewType
3. WHEN the step function resumes THEN the system SHALL validate the updated reviewType against allowed values
4. IF the updated reviewType is invalid THEN the system SHALL return an error and allow another reassignment attempt

### Requirement 3

**User Story:** As a loan review system, I want to fetch attribute decisions from the PostgreSQL database, so that loan decisions are based on persisted attribute data.

#### Acceptance Criteria

1. WHEN the workflow needs to determine loan decision THEN the system SHALL connect to the PostgreSQL database
2. WHEN connected to PostgreSQL THEN the system SHALL query the loan_attributes table using the LoanNumber as the primary lookup key
3. WHEN querying loan_attributes THEN the system SHALL retrieve all attribute records for the specified loan
4. WHEN attribute records are retrieved THEN the system SHALL extract the status field from each record
5. WHEN extracting attribute status THEN the system SHALL validate that each status value is one of ['Pending', 'Reclass', 'Approved', 'Rejected', 'Repurchase'] as defined in loan-ppa-request.schema.json
6. IF any attribute status value is not in the allowed enum THEN the system SHALL log an error and treat the attribute as invalid
7. WHEN all attributes are successfully retrieved and validated THEN the system SHALL proceed to loan decision determination logic
8. IF the database connection fails THEN the system SHALL enter an error state and not proceed further
9. IF no attributes are found for the loan THEN the system SHALL log a warning and treat the loan as having no attribute decisions

### Requirement 4

**User Story:** As a loan review system, I want to determine the final loan status based on attribute decisions from the database, so that the appropriate downstream action is triggered.

#### Acceptance Criteria

1. WHEN all attributes retrieved from loan_attributes table have status 'Approved' THEN the system SHALL set the loan decision to 'Approved'
2. WHEN at least one attribute has status 'Approved' AND at least one attribute has status 'Rejected' THEN the system SHALL set the loan decision to 'Partially Processed'
3. WHEN all attributes retrieved from loan_attributes table have status 'Rejected' THEN the system SHALL set the loan decision to 'Rejected'
4. WHEN at least one attribute has status 'Repurchase' THEN the system SHALL set the loan decision to 'Repurchase'
5. WHEN at least one attribute has status 'Reclass' THEN the system SHALL set the loan decision to 'Reclass'
6. WHEN any attribute has status 'Pending' THEN the system SHALL set the loan decision to 'Pending Review'
7. WHEN determining loan decision THEN the system SHALL use the attribute status values retrieved from the PostgreSQL loan_attributes table as the source of truth

### Requirement 5

**User Story:** As a loan review system, I want to route approved or denied loans to the Vend PPA system, so that decisions are recorded in the Business Engine.

#### Acceptance Criteria

1. WHEN the loan decision determined from loan_attributes table is 'Approved' or 'Rejected' THEN the system SHALL make a call to the Vend PPA API with the loan decision
2. WHEN the Vend PPA API call succeeds THEN the system SHALL complete the workflow successfully
3. IF the Vend PPA API call fails THEN the system SHALL enter an error state and not proceed further
4. WHEN calling Vend PPA THEN the system SHALL include the complete loan state including all attribute decisions retrieved from the PostgreSQL loan_attributes table in the API request

### Requirement 6

**User Story:** As a loan review system, I want to notify stakeholders of repurchase decisions, so that appropriate actions can be taken.

#### Acceptance Criteria

1. WHEN the loan decision is 'Repurchase' THEN the system SHALL send an email notification to a configured email address
2. WHEN sending the repurchase notification THEN the system SHALL use a predefined email template from a configuration service
3. IF the email send fails THEN the system SHALL log the error and complete the workflow successfully
4. WHEN the repurchase notification is sent THEN the system SHALL complete the workflow successfully

### Requirement 7

**User Story:** As a loan review system, I want to handle reclass approved decisions with human confirmation, so that reclassification can be confirmed before final processing.

#### Acceptance Criteria

1. WHEN the loan decision determined from loan_attributes table is 'Reclass' THEN the system SHALL pause and wait for human confirmation
2. WHEN a human confirms the reclass decision THEN the system SHALL call the Vend PPA API with the reclass status and all attribute decisions from the loan_attributes table
3. WHEN the Vend PPA API call succeeds THEN the system SHALL complete the workflow successfully
4. IF the Vend PPA API call fails THEN the system SHALL enter an error state and not proceed further

### Requirement 8

**User Story:** As a loan review system, I want to maintain complete state throughout the workflow, so that all decisions and transitions are traceable.

#### Acceptance Criteria

1. WHEN the step function executes THEN the system SHALL maintain the complete state including requestNumber, loanNumber, loanDecision, attributes list, currentAssignedUsername, and reviewType
2. WHEN state transitions occur THEN the system SHALL preserve all previous state information
3. WHEN the workflow completes THEN the system SHALL have a complete audit trail of all state changes

### Requirement 9

**User Story:** As a business rules engine, I want to start a new PPA review workflow instance, so that manual decisioning can be initiated for loans requiring review.

#### Acceptance Criteria

1. WHEN the business rules engine calls the startPPAreview operation with a valid payload conforming to loan-ppa-request.schema.json THEN the system SHALL create a new workflow execution instance
2. WHEN the startPPAreview operation receives a request with TaskId, RequestNumber, LoanNumber, ReviewType, ReviewStepUserId, SelectionCriteria, LoanDecision, and Attributes fields THEN the system SHALL validate all fields are present and conform to the schema definition in loan-ppa-request.schema.json
3. WHEN the ReviewType field contains a value from the enum ['LOC', 'Sec Policy', 'Conduit'] as defined in loan-ppa-request.schema.json THEN the system SHALL accept the value and initialize the workflow with that review type
4. WHEN the LoanNumber field is provided THEN the system SHALL validate it matches the pattern [0-9]{10}$ as defined in loan-ppa-request.schema.json
5. WHEN the Attributes array is provided THEN the system SHALL validate each attribute contains Name and Decision fields as defined in loan-ppa-request.schema.json, and Decision is one of ['Pending', 'Reclass', 'Approved', 'Rejected', 'Repurchase']
6. WHEN the startPPAreview operation completes successfully THEN the system SHALL return a response conforming to loan-ppa-workflow-response.schema.json containing the workflow execution details including TaskNumber, RequestNumber, LoanNumber, LoanDecision, Attributes, ReviewStep, ReviewStepUserId, WorkflowStateName, and StateTransitionHistory
7. WHEN the startPPAreview operation is invoked THEN the system SHALL initialize the StateTransitionHistory array with the initial workflow state transition as defined in loan-ppa-workflow-response.schema.json
8. IF any required field is missing, invalid, or does not conform to loan-ppa-request.schema.json THEN the system SHALL return an error response with a descriptive error message and not create a workflow execution

### Requirement 10

**User Story:** As a workflow system, I want to track and return the complete state transition history, so that all workflow state changes are auditable and retrievable.

#### Acceptance Criteria

1. WHEN a workflow state transition occurs THEN the system SHALL record the WorkflowStateName, WorkflowStateUserId, WorkflowStateStartDateTime, and WorkflowStateEndDateTime
2. WHEN the startPPAreview operation returns a response THEN the system SHALL include the StateTransitionHistory array containing all state transitions executed so far
3. WHEN the StateTransitionHistory is populated THEN the system SHALL include the datetime format as ISO 8601 time format for all timestamp fields
4. WHEN the workflow completes THEN the system SHALL have a complete StateTransitionHistory showing all states from initialization through completion

