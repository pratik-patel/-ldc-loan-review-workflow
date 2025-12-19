package com.ldc.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for logging audit trail and state transitions to DynamoDB.
 * Provides compliance and debugging capabilities.
 */
@Service
public class AuditTrailService {

    private static final Logger logger = LoggerFactory.getLogger(AuditTrailService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DynamoDbClient dynamoDbClient;
    private final String auditTableName;

    public AuditTrailService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.auditTableName = System.getenv("AUDIT_TABLE_NAME");
        if (this.auditTableName == null || this.auditTableName.isEmpty()) {
            logger.warn("AUDIT_TABLE_NAME environment variable not set, audit logging will use main state table");
        }
    }

    /**
     * Log a state transition to DynamoDB.
     */
    public void logStateTransition(String requestNumber, String loanNumber, String executionId,
                                   String stateChange, String details, String timestamp) {
        try {
            String auditId = UUID.randomUUID().toString();
            
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("auditId", AttributeValue.builder().s(auditId).build());
            item.put("requestNumber", AttributeValue.builder().s(requestNumber).build());
            item.put("loanNumber", AttributeValue.builder().s(loanNumber).build());
            item.put("executionId", AttributeValue.builder().s(executionId).build());
            item.put("stateChange", AttributeValue.builder().s(stateChange).build());
            item.put("timestamp", AttributeValue.builder().s(timestamp).build());
            
            if (details != null && !details.isEmpty()) {
                item.put("details", AttributeValue.builder().s(details).build());
            }

            // Use audit table if available, otherwise use main state table
            String tableName = auditTableName != null ? auditTableName : System.getenv("DYNAMODB_TABLE");
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);
            
            logger.info("Audit trail logged: requestNumber={}, stateChange={}, timestamp={}", 
                    requestNumber, stateChange, timestamp);
        } catch (Exception e) {
            logger.error("Error logging audit trail for requestNumber: {}", requestNumber, e);
            // Non-blocking error - don't throw
        }
    }

    /**
     * Log a workflow completion.
     */
    public void logWorkflowCompletion(String requestNumber, String loanNumber, String executionId,
                                      String finalStatus, String timestamp) {
        logStateTransition(requestNumber, loanNumber, executionId, 
                "WorkflowCompleted", "finalStatus=" + finalStatus, timestamp);
    }

    /**
     * Log a workflow error.
     */
    public void logWorkflowError(String requestNumber, String loanNumber, String executionId,
                                 String errorMessage, String timestamp) {
        logStateTransition(requestNumber, loanNumber, executionId, 
                "WorkflowError", "error=" + errorMessage, timestamp);
    }
}
