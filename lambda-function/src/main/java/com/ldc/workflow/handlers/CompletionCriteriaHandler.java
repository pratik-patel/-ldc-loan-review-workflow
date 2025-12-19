package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldc.workflow.business.CompletionCriteriaChecker;
import com.ldc.workflow.types.LoanAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Lambda handler for completion criteria validation.
 * Checks if loan decision is complete (all attributes non-null/non-Pending and loan decision non-null).
 * 
 * Input: JSON with requestNumber, loanNumber, loanDecision, attributes
 * Output: JSON with completion status and blocking reasons if incomplete
 */
@Component("completionCriteriaHandler")
public class CompletionCriteriaHandler implements Function<JsonNode, JsonNode> {

    private static final Logger logger = LoggerFactory.getLogger(CompletionCriteriaHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CompletionCriteriaChecker completionCriteriaChecker;

    public CompletionCriteriaHandler(CompletionCriteriaChecker completionCriteriaChecker) {
        this.completionCriteriaChecker = completionCriteriaChecker;
    }

    @Override
    public JsonNode apply(JsonNode input) {
        try {
            logger.info("Completion Criteria handler invoked");

            // Extract input fields
            String requestNumber = input.get("requestNumber").asText("unknown");
            String loanNumber = input.get("loanNumber").asText("unknown");
            String loanDecision = input.has("loanDecision") && !input.get("loanDecision").isNull() ? 
                    input.get("loanDecision").asText() : null;

            logger.debug("Checking completion criteria for requestNumber: {}, loanNumber: {}", 
                    requestNumber, loanNumber);

            // Extract attributes from input
            List<LoanAttribute> attributes = extractAttributes(input);

            // Check completion criteria
            boolean isComplete = completionCriteriaChecker.isLoanDecisionComplete(
                    loanDecision, attributes);

            logger.info("Loan decision completion status: {} for requestNumber: {}", 
                    isComplete, requestNumber);

            if (isComplete) {
                return createSuccessResponse(requestNumber, loanNumber, true, null);
            } else {
                String reason = completionCriteriaChecker.getIncompleteReason(
                        loanDecision, attributes);
                return createSuccessResponse(requestNumber, loanNumber, false, reason);
            }
        } catch (Exception e) {
            logger.error("Error in completion criteria handler", e);
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
                                          boolean isComplete, String blockingReason) {
        var response = objectMapper.createObjectNode()
                .put("success", true)
                .put("requestNumber", requestNumber)
                .put("loanNumber", loanNumber)
                .put("complete", isComplete);

        if (blockingReason != null) {
            response.put("blockingReasons", blockingReason);
        }

        return response;
    }

    private JsonNode createErrorResponse(String requestNumber, String loanNumber, String error) {
        return objectMapper.createObjectNode()
                .put("success", false)
                .put("requestNumber", requestNumber)
                .put("loanNumber", loanNumber)
                .put("error", error);
    }
}
