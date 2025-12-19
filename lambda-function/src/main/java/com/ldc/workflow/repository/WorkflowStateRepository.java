package com.ldc.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldc.workflow.types.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for persisting and retrieving workflow state from DynamoDB.
 * Handles all DynamoDB operations for the loan review workflow.
 */
@Repository
public class WorkflowStateRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStateRepository.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public WorkflowStateRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = System.getenv("DYNAMODB_TABLE");
        if (this.tableName == null || this.tableName.isEmpty()) {
            throw new IllegalArgumentException("DYNAMODB_TABLE environment variable is required");
        }
    }

    /**
     * Save or update workflow state in DynamoDB.
     */
    public void save(WorkflowState state) {
        try {
            // Set timestamps
            if (state.getCreatedAt() == null) {
                state.setCreatedAt(Instant.now().toString());
            }
            state.setUpdatedAt(Instant.now().toString());

            // Convert state to DynamoDB item
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("requestNumber", AttributeValue.builder().s(state.getRequestNumber()).build());
            item.put("executionId", AttributeValue.builder().s(state.getExecutionId()).build());
            item.put("loanNumber", AttributeValue.builder().s(state.getLoanNumber()).build());
            item.put("reviewType", AttributeValue.builder().s(state.getReviewType()).build());
            item.put("createdAt", AttributeValue.builder().s(state.getCreatedAt()).build());
            item.put("updatedAt", AttributeValue.builder().s(state.getUpdatedAt()).build());
            item.put("status", AttributeValue.builder().s(state.getStatus() != null ? state.getStatus() : "PENDING").build());

            // Optional fields
            if (state.getLoanDecision() != null) {
                item.put("loanDecision", AttributeValue.builder().s(state.getLoanDecision()).build());
            }
            if (state.getLoanStatus() != null) {
                item.put("loanStatus", AttributeValue.builder().s(state.getLoanStatus()).build());
            }
            if (state.getCurrentAssignedUsername() != null) {
                item.put("currentAssignedUsername", AttributeValue.builder().s(state.getCurrentAssignedUsername()).build());
            }
            if (state.getTaskToken() != null) {
                item.put("taskToken", AttributeValue.builder().s(state.getTaskToken()).build());
            }
            if (state.getAttributes() != null) {
                String attributesJson = objectMapper.writeValueAsString(state.getAttributes());
                item.put("attributes", AttributeValue.builder().s(attributesJson).build());
            }

            // Put item in DynamoDB
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);
            logger.info("Saved workflow state for requestNumber: {}, executionId: {}", 
                    state.getRequestNumber(), state.getExecutionId());
        } catch (Exception e) {
            logger.error("Error saving workflow state for requestNumber: {}", state.getRequestNumber(), e);
            throw new RuntimeException("Failed to save workflow state", e);
        }
    }

    /**
     * Retrieve workflow state by requestNumber and executionId.
     */
    public Optional<WorkflowState> findByRequestNumberAndExecutionId(String requestNumber, String executionId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "requestNumber", AttributeValue.builder().s(requestNumber).build(),
                            "executionId", AttributeValue.builder().s(executionId).build()
                    ))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item() == null || response.item().isEmpty()) {
                logger.debug("No workflow state found for requestNumber: {}, executionId: {}", 
                        requestNumber, executionId);
                return Optional.empty();
            }

            WorkflowState state = convertItemToWorkflowState(response.item());
            logger.debug("Retrieved workflow state for requestNumber: {}, executionId: {}", 
                    requestNumber, executionId);
            return Optional.of(state);
        } catch (Exception e) {
            logger.error("Error retrieving workflow state for requestNumber: {}, executionId: {}", 
                    requestNumber, executionId, e);
            throw new RuntimeException("Failed to retrieve workflow state", e);
        }
    }

    /**
     * Retrieve the most recent workflow state by loanNumber.
     */
    public Optional<WorkflowState> findMostRecentByLoanNumber(String loanNumber) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("loanNumber-createdAt-index")
                    .keyConditionExpression("loanNumber = :loanNumber")
                    .expressionAttributeValues(Map.of(
                            ":loanNumber", AttributeValue.builder().s(loanNumber).build()
                    ))
                    .scanIndexForward(false) // Sort by createdAt descending
                    .limit(1)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            if (response.items() == null || response.items().isEmpty()) {
                logger.debug("No workflow state found for loanNumber: {}", loanNumber);
                return Optional.empty();
            }

            WorkflowState state = convertItemToWorkflowState(response.items().get(0));
            logger.debug("Retrieved most recent workflow state for loanNumber: {}", loanNumber);
            return Optional.of(state);
        } catch (Exception e) {
            logger.error("Error retrieving workflow state for loanNumber: {}", loanNumber, e);
            throw new RuntimeException("Failed to retrieve workflow state", e);
        }
    }

    /**
     * Convert DynamoDB item to WorkflowState object.
     */
    private WorkflowState convertItemToWorkflowState(Map<String, AttributeValue> item) throws Exception {
        WorkflowState state = new WorkflowState();
        state.setRequestNumber(item.get("requestNumber").s());
        state.setExecutionId(item.get("executionId").s());
        state.setLoanNumber(item.get("loanNumber").s());
        state.setReviewType(item.get("reviewType").s());
        state.setCreatedAt(item.get("createdAt").s());
        state.setUpdatedAt(item.get("updatedAt").s());
        state.setStatus(item.get("status").s());

        if (item.containsKey("loanDecision") && item.get("loanDecision") != null) {
            state.setLoanDecision(item.get("loanDecision").s());
        }
        if (item.containsKey("loanStatus") && item.get("loanStatus") != null) {
            state.setLoanStatus(item.get("loanStatus").s());
        }
        if (item.containsKey("currentAssignedUsername") && item.get("currentAssignedUsername") != null) {
            state.setCurrentAssignedUsername(item.get("currentAssignedUsername").s());
        }
        if (item.containsKey("taskToken") && item.get("taskToken") != null) {
            state.setTaskToken(item.get("taskToken").s());
        }
        if (item.containsKey("attributes") && item.get("attributes") != null) {
            String attributesJson = item.get("attributes").s();
            state.setAttributes(objectMapper.readValue(attributesJson, 
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, 
                            com.ldc.workflow.types.LoanAttribute.class)));
        }

        return state;
    }
}
