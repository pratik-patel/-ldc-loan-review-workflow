package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ldc.workflow.business.CompletionCriteriaChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CompletionCriteriaHandler
 * Tests loan decision completion criteria validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompletionCriteriaHandler Tests")
class CompletionCriteriaHandlerTest {

    private ObjectMapper objectMapper;
    private CompletionCriteriaHandler handler;
    private CompletionCriteriaChecker completionCriteriaChecker;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        completionCriteriaChecker = new CompletionCriteriaChecker();
        handler = new CompletionCriteriaHandler(completionCriteriaChecker);
    }

    @Test
    @DisplayName("Should mark complete when all attributes are non-Pending and loanDecision is set")
    void testCompleteWhenAllAttributesNonPending() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Rejected"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertTrue(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete when any attribute is Pending")
    void testIncompleteWhenAttributeIsPending() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Pending"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete when loanDecision is null")
    void testIncompleteWhenLoanDecisionIsNull() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.putNull("loanDecision");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Approved"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete when loanDecision is missing")
    void testIncompleteWhenLoanDecisionIsMissing() {
        // Arrange
        ObjectNode input = createBaseInput();
        // Don't set loanDecision
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Approved"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete when any attribute is null")
    void testIncompleteWhenAttributeIsNull() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        ObjectNode attr = objectMapper.createObjectNode();
        attr.put("attributeName", "CreditScore");
        attr.putNull("attributeDecision");
        attributes.add(attr);
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete with empty attributes array")
    void testCompleteWithEmptyAttributesArray() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark complete with single non-Pending attribute")
    void testCompleteWithSingleAttribute() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertTrue(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete with single Pending attribute")
    void testIncompleteWithSinglePendingAttribute() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Pending"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should provide blocking reasons when incomplete")
    void testBlockingReasonsWhenIncomplete() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.putNull("loanDecision");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Pending"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
        assertTrue(result.has("blockingReasons"));
    }

    @Test
    @DisplayName("Should preserve requestNumber in response")
    void testPreserveRequestNumber() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("requestNumber", "REQ-PRESERVE-001");
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertEquals("REQ-PRESERVE-001", result.get("requestNumber").asText());
    }

    @Test
    @DisplayName("Should handle all valid loan decisions")
    void testAllValidLoanDecisions() {
        String[] decisions = {"Approved", "Rejected", "Repurchase", "Reclass"};
        
        for (String decision : decisions) {
            // Arrange
            ObjectNode input = createBaseInput();
            input.put("loanDecision", decision);
            ArrayNode attributes = objectMapper.createArrayNode();
            attributes.add(createAttribute("CreditScore", "Approved"));
            input.set("attributes", attributes);

            // Act
            JsonNode result = handler.apply(input);

            // Assert
            assertTrue(result.get("complete").asBoolean(), 
                "Should be complete for decision: " + decision);
        }
    }

    @Test
    @DisplayName("Should handle multiple Pending attributes")
    void testMultiplePendingAttributes() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Pending"));
        attributes.add(createAttribute("DebtRatio", "Pending"));
        attributes.add(createAttribute("IncomeVerification", "Pending"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should handle mixed Pending and non-Pending attributes")
    void testMixedPendingAttributes() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Pending"));
        attributes.add(createAttribute("IncomeVerification", "Rejected"));
        input.set("attributes", attributes);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    @Test
    @DisplayName("Should mark incomplete when attributes array is null")
    void testNullAttributesArray() {
        // Arrange
        ObjectNode input = createBaseInput();
        input.put("loanDecision", "Approved");
        input.putNull("attributes");

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("complete").asBoolean());
    }

    // Helper methods
    private ObjectNode createBaseInput() {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-001");
        input.put("loanNumber", "LOAN-001");
        return input;
    }

    private ObjectNode createAttribute(String name, String decision) {
        ObjectNode attr = objectMapper.createObjectNode();
        attr.put("attributeName", name);
        attr.put("attributeDecision", decision);
        return attr;
    }
}
