package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldc.workflow.business.LoanStatusDeterminer;
import com.ldc.workflow.types.LoanAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Lambda handler for loan status determination.
 * Determines the final loan status based on attribute decisions.
 * For "Reclass Approved" status, creates a DynamoDB record to track reclass confirmation.
 * 
 * Input: JSON with requestNumber, loanNumber, attributes, executionId
 * Output: JSON with determined loan status
 */
@Component("loanStatusDeterminationHandler")
public class LoanStatusDeterminationHandler implements Function<JsonNode, JsonNode> {

    private static final Logger logger = LoggerFactory.getLogger(LoanStatusDeterminationHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String RECLASS_APPROVED = "Reclass Approved";

    private final LoanStatusDeterminer loanStatusDeterminer;
    private final DynamoDbClient dynamoDbClient;
    private final String dynamoDbTableName;

    public LoanStatusDeterminationHandler(LoanStatusDeterminer loanStatusDeterminer, 
                                        DynamoDbClient dynamoDbClient) {
        this.loanStatusDeterminer = loanStatusDeterminer;
        this.dynamoDbClient = dynamoDbClient;
        this.dynamoDbTableName = System.getenv("DYNAMODB_TABLE");
    }

    @Override
    public JsonNode apply(JsonNode input) {
        try {
            logger.info("Loan Status Determination handler invoked");

            // Extract input fields
            String requestNumber = input.get("requestNumber").asText("unknown");
            String loanNumber = input.get("loanNumber").asText("unknown");
            String executionId = input.get("executionId").asText("unknown");

            logger.debug("Determining loan status for requestNumber: {}, loanNumber: {}", 
                    requestNumber, loanNumber);

            // Extract attributes from input
            List<LoanAttribute> attributes = extractAttributes(input);
            if (attributes.isEmpty()) {
                logger.warn("No attributes found for loan status determination");
                return createErrorResponse(requestNumber, loanNumber, 
                        "No attributes found");
            }

            // Determine loan status
            String loanStatus = loanStatusDeterminer.determineStatus(attributes);
            logger.info("Loan status determined: {} for requestNumber: {}", loanStatus, requestNumber);

            // If reclass approved, create DynamoDB record to track reclass confirmation
            if (RECLASS_APPROVED.equals(loanStatus)) {
                createReclassRecord(requestNumber, executionId, loanNumber);
            }

            // Return success response
            return createSuccessResponse(requestNumber, loanNumber, loanStatus, attributes);
        } catch (Exception e) {
            logger.error("Error in loan status determination handler", e);
            return createErrorResponse("unknown", "unknown", 
                    "Internal error: " + e.getMessage());
        }
    }

    private List<LoanAttribute> extractAttributes(JsonNode input) {
        List<LoanAttribute> attributes = new ArrayList<>();
        
        if (!input.has("attributes") || input.get("attributes").isNull()) {
            return attributes;
        }

        JsonNode attributesNode = input.get("attributes");
        if (!attributesNode.isArray()) {
            return attributes;
        }

        for (JsonNode attrNode : attributesNode) {
            String name = attrNode.has("attributeName") ? attrNode.get("attributeName").asText() : null;
            String decision = attrNode.has("attributeDecision") && !attrNode.get("attributeDecision").isNull() ? 
                    attrNode.get("attributeDecision").asText() : null;
            
            if (name != null) {
                LoanAttribute attr = new LoanAttribute();
                attr.setAttributeName(name);
                attr.setAttributeDecision(decision);
                attributes.add(attr);
            }
        }

        return attributes;
    }

    private JsonNode createSuccessResponse(String requestNumber, String loanNumber, 
                                          String loanStatus, List<LoanAttribute> attributes) {
        return objectMapper.createObjectNode()
                .put("success", true)
                .put("requestNumber", requestNumber)
                .put("loanNumber", loanNumber)
                .put("status", loanStatus)
                .put("attributeCount", attributes.size());
    }

    private JsonNode createErrorResponse(String requestNumber, String loanNumber, String error) {
        return objectMapper.createObjectNode()
                .put("success", false)
                .put("requestNumber", requestNumber)
                .put("loanNumber", loanNumber)
                .put("error", error);
    }

    /**
     * Creates a DynamoDB record to track reclass confirmation status.
     * This record is checked after the reclass timer expires to determine
     * if the reclass was confirmed or if it expired.
     */
    private void createReclassRecord(String requestNumber, String executionId, String loanNumber) {
        try {
            logger.info("Creating reclass confirmation record for requestNumber: {}, executionId: {}", 
                    requestNumber, executionId);

            Map<String, AttributeValue> item = new HashMap<>();
            
            // Primary key
            item.put("requestNumber", AttributeValue.builder().s(requestNumber).build());
            item.put("executionId", AttributeValue.builder().s(executionId).build());
            
            // Additional attributes
            item.put("loanNumber", AttributeValue.builder().s(loanNumber).build());
            item.put("reclassConfirmed", AttributeValue.builder().bool(false).build());
            item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
            item.put("status", AttributeValue.builder().s("PENDING").build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(dynamoDbTableName)
                    .item(item)
                    .build();

            PutItemResponse response = dynamoDbClient.putItem(putItemRequest);
            logger.info("Reclass confirmation record created successfully for requestNumber: {}", requestNumber);
        } catch (Exception e) {
            logger.error("Error creating reclass confirmation record for requestNumber: {}", requestNumber, e);
            // Don't throw - log and continue, as this is not critical to workflow continuation
        }
    }
}
