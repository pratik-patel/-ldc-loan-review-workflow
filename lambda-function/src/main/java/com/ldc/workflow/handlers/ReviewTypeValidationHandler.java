package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldc.workflow.repository.WorkflowStateRepository;
import com.ldc.workflow.types.WorkflowState;
import com.ldc.workflow.validation.ReviewTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Lambda handler for review type validation.
 * Validates that the review type is one of the allowed values and stores it in
 * DynamoDB.
 * 
 * Input: JSON with requestNumber, loanNumber, reviewType, attributes,
 * loanDecision, currentAssignedUsername
 * Output: JSON with validation result and updated workflow state
 */
@Component("reviewTypeValidationHandler")
public class ReviewTypeValidationHandler implements Function<JsonNode, JsonNode> {

    private static final Logger logger = LoggerFactory.getLogger(ReviewTypeValidationHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ReviewTypeValidator reviewTypeValidator;
    private final WorkflowStateRepository workflowStateRepository;

    public ReviewTypeValidationHandler(ReviewTypeValidator reviewTypeValidator,
            WorkflowStateRepository workflowStateRepository) {
        this.reviewTypeValidator = reviewTypeValidator;
        this.workflowStateRepository = workflowStateRepository;
    }

    @Override
    public JsonNode apply(JsonNode input) {
        try {
            logger.info("Review Type Validation handler invoked");

            // Validate required fields exist
            if (!input.has("requestNumber") || input.get("requestNumber").isNull()) {
                logger.warn("Missing required field: requestNumber");
                return createErrorResponse("unknown", "unknown", "unknown", "Missing required field: requestNumber");
            }
            if (!input.has("loanNumber") || input.get("loanNumber").isNull()) {
                logger.warn("Missing required field: loanNumber");
                return createErrorResponse("unknown", "unknown", "unknown", "Missing required field: loanNumber");
            }
            if (!input.has("reviewType") || input.get("reviewType").isNull()) {
                logger.warn("Missing required field: reviewType");
                return createErrorResponse("unknown", "unknown", "unknown", "Missing required field: reviewType");
            }

            // Extract input fields
            String requestNumber = input.get("requestNumber").asText();
            String loanNumber = input.get("loanNumber").asText();
            String reviewType = input.get("reviewType").asText();
            String executionId = input.has("executionId") ? input.get("executionId").asText()
                    : "ldc-loan-review-" + requestNumber;

            logger.debug("Validating review type: {} for requestNumber: {}", reviewType, requestNumber);

            // Validate review type
            if (!reviewTypeValidator.isValid(reviewType)) {
                logger.warn("Invalid review type: {}", reviewType);
                return createErrorResponse(requestNumber, loanNumber, reviewType,
                        reviewTypeValidator.getErrorMessage(reviewType));
            }

            // Create or update workflow state
            WorkflowState state = new WorkflowState();
            state.setRequestNumber(requestNumber);
            state.setLoanNumber(loanNumber);
            state.setReviewType(reviewType);
            state.setExecutionId(executionId);
            state.setStatus("PENDING");

            // Copy optional fields from input
            if (input.has("loanDecision") && !input.get("loanDecision").isNull()) {
                state.setLoanDecision(input.get("loanDecision").asText());
            }
            if (input.has("currentAssignedUsername") && !input.get("currentAssignedUsername").isNull()) {
                state.setCurrentAssignedUsername(input.get("currentAssignedUsername").asText());
            }
            if (input.has("attributes") && !input.get("attributes").isNull()) {
                state.setAttributes(objectMapper.readValue(
                        objectMapper.writeValueAsString(input.get("attributes")),
                        objectMapper.getTypeFactory().constructCollectionType(java.util.List.class,
                                com.ldc.workflow.types.LoanAttribute.class)));
            }

            // Save to DynamoDB
            workflowStateRepository.save(state);
            logger.info("Review type validated and stored successfully for requestNumber: {}", requestNumber);

            // Return success response
            return createSuccessResponse(state);
        } catch (Exception e) {
            logger.error("Error in review type validation handler", e);
            return createErrorResponse("unknown", "unknown", "unknown",
                    "Internal error: " + e.getMessage());
        }
    }

    private JsonNode createSuccessResponse(WorkflowState state) {
        return objectMapper.createObjectNode()
                .put("success", true)
                .put("requestNumber", state.getRequestNumber())
                .put("loanNumber", state.getLoanNumber())
                .put("reviewType", state.getReviewType())
                .put("executionId", state.getExecutionId())
                .put("isValid", true)
                .put("message", "Review type validated successfully");
    }

    private JsonNode createErrorResponse(String requestNumber, String loanNumber, String reviewType, String error) {
        JsonNode allowedValues = objectMapper.createArrayNode();
        try {
            if (reviewTypeValidator != null) {
                allowedValues = objectMapper.valueToTree(reviewTypeValidator.getAllowedReviewTypes());
            }
        } catch (Exception e) {
            logger.warn("Failed to get allowed review types", e);
        }

        JsonNode response = objectMapper.createObjectNode()
                .put("success", false)
                .put("requestNumber", requestNumber)
                .put("loanNumber", loanNumber)
                .put("reviewType", reviewType)
                .put("isValid", false)
                .put("error", error)
                .set("allowedValues", allowedValues);

        return response;
    }
}
