package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldc.workflow.service.AuditTrailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Function;

/**
 * Handler for logging audit trail and state transitions.
 * Logs all state changes to DynamoDB for compliance and debugging.
 * 
 * Input: JSON with requestNumber, loanNumber, executionId, stateChange, and optional details
 * Output: JSON with audit logging status
 * 
 * Requirements: 8.2, 8.3
 */
@Component("auditTrailHandler")
public class AuditTrailHandler implements Function<JsonNode, JsonNode> {

    private static final Logger logger = LoggerFactory.getLogger(AuditTrailHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AuditTrailService auditTrailService;

    public AuditTrailHandler(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public JsonNode apply(JsonNode input) {
        try {
            logger.info("Audit Trail handler invoked");

            // Extract input fields
            String requestNumber = input.get("requestNumber").asText();
            String loanNumber = input.get("loanNumber").asText();
            String executionId = input.has("executionId") ? input.get("executionId").asText() : "unknown";
            String stateChange = input.get("stateChange").asText();
            String details = input.has("details") ? input.get("details").asText() : null;

            logger.debug("Logging audit trail for requestNumber: {}, stateChange: {}", 
                    requestNumber, stateChange);

            // Log state transition
            auditTrailService.logStateTransition(
                    requestNumber,
                    loanNumber,
                    executionId,
                    stateChange,
                    details,
                    Instant.now().toString()
            );

            logger.info("Audit trail logged successfully for requestNumber: {}, stateChange: {}", 
                    requestNumber, stateChange);

            return createSuccessResponse(requestNumber, stateChange);
        } catch (Exception e) {
            logger.error("Error in audit trail handler", e);
            // Non-blocking error - log but don't fail workflow
            return createErrorResponse("unknown", "Error logging audit trail: " + e.getMessage());
        }
    }

    private JsonNode createSuccessResponse(String requestNumber, String stateChange) {
        return objectMapper.createObjectNode()
                .put("success", true)
                .put("requestNumber", requestNumber)
                .put("stateChange", stateChange)
                .put("message", "Audit trail logged successfully");
    }

    private JsonNode createErrorResponse(String requestNumber, String error) {
        return objectMapper.createObjectNode()
                .put("success", false)
                .put("requestNumber", requestNumber)
                .put("error", error);
    }
}
