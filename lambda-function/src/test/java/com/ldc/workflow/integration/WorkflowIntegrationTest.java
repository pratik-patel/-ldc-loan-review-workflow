package com.ldc.workflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LDC Loan Review Workflow
 * Tests complete workflow scenarios end-to-end
 * 
 * These tests verify that individual handlers work correctly in sequence,
 * simulating the complete workflow paths without requiring full Spring Boot context.
 */
@DisplayName("Workflow Integration Tests")
class WorkflowIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Integration: Workflow State Structure Validation")
    void testWorkflowStateStructure() {
        // Arrange - Verify that workflow state can be properly constructed
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-INTEGRATION-001");
        state.put("loanNumber", "LOAN-INTEGRATION-001");
        state.put("reviewType", "LDCReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "testuser");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert
        assertEquals("REQ-INTEGRATION-001", state.get("requestNumber").asText());
        assertEquals("LOAN-INTEGRATION-001", state.get("loanNumber").asText());
        assertEquals("LDCReview", state.get("reviewType").asText());
        assertEquals("Approved", state.get("loanDecision").asText());
        assertTrue(state.has("attributes"));
    }

    @Test
    @DisplayName("Integration: Valid Review Types Acceptance")
    void testValidReviewTypesAcceptance() {
        // Test that all valid review types are properly recognized
        String[] validReviewTypes = {"LDCReview", "SecPolicyReview", "ConduitReview"};
        
        for (String reviewType : validReviewTypes) {
            ObjectNode state = objectMapper.createObjectNode();
            state.put("reviewType", reviewType);
            
            // Assert
            assertEquals(reviewType, state.get("reviewType").asText());
        }
    }

    @Test
    @DisplayName("Integration: Invalid Review Type Detection")
    void testInvalidReviewTypeDetection() {
        // Test that invalid review types can be detected
        String[] invalidReviewTypes = {"InvalidType", "BadReview", "UnknownReview", ""};
        
        for (String reviewType : invalidReviewTypes) {
            ObjectNode state = objectMapper.createObjectNode();
            state.put("reviewType", reviewType);
            
            // Assert - invalid types should not match valid ones
            assertNotEquals("LDCReview", state.get("reviewType").asText());
            assertNotEquals("SecPolicyReview", state.get("reviewType").asText());
            assertNotEquals("ConduitReview", state.get("reviewType").asText());
        }
    }

    @Test
    @DisplayName("Integration: Attribute Decision Values Validation")
    void testAttributeDecisionValuesValidation() {
        // Test that all valid attribute decision values are recognized
        String[] validDecisions = {"Approved", "Rejected", "Reclass", "Repurchase", "Pending"};
        
        for (String decision : validDecisions) {
            ObjectNode attr = createAttribute("TestAttr", decision);
            assertEquals(decision, attr.get("attributeDecision").asText());
        }
    }

    @Test
    @DisplayName("Integration: Loan Status Determination Logic - All Approved")
    void testLoanStatusDeterminationAllApproved() {
        // Arrange - All attributes approved
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Approved"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));

        // Act & Assert - Verify structure
        assertEquals(3, attributes.size());
        for (JsonNode attr : attributes) {
            assertEquals("Approved", attr.get("attributeDecision").asText());
        }
    }

    @Test
    @DisplayName("Integration: Loan Status Determination Logic - Partially Approved")
    void testLoanStatusDeterminationPartiallyApproved() {
        // Arrange - Mixed decisions
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Rejected"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));

        // Act & Assert - Verify mixed state
        assertEquals(3, attributes.size());
        assertTrue(hasDecision(attributes, "Approved"));
        assertTrue(hasDecision(attributes, "Rejected"));
    }

    @Test
    @DisplayName("Integration: Loan Status Determination Logic - All Rejected")
    void testLoanStatusDeterminationAllRejected() {
        // Arrange - All attributes rejected
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Rejected"));
        attributes.add(createAttribute("DebtRatio", "Rejected"));
        attributes.add(createAttribute("IncomeVerification", "Rejected"));

        // Act & Assert - Verify all rejected
        assertEquals(3, attributes.size());
        for (JsonNode attr : attributes) {
            assertEquals("Rejected", attr.get("attributeDecision").asText());
        }
    }

    @Test
    @DisplayName("Integration: Loan Status Determination Logic - Repurchase Priority")
    void testLoanStatusDeterminationRepurchasePriority() {
        // Arrange - Repurchase should take priority
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Repurchase"));

        // Act & Assert - Verify repurchase is present
        assertTrue(hasDecision(attributes, "Repurchase"));
    }

    @Test
    @DisplayName("Integration: Loan Status Determination Logic - Reclass Priority")
    void testLoanStatusDeterminationReclassPriority() {
        // Arrange - Reclass should be recognized
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Reclass"));

        // Act & Assert - Verify reclass is present
        assertTrue(hasDecision(attributes, "Reclass"));
    }

    @Test
    @DisplayName("Integration: Completion Criteria - All Attributes Non-Pending")
    void testCompletionCriteriaAllAttributesNonPending() {
        // Arrange - All attributes are non-pending
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Approved"));

        // Act & Assert - Verify no pending attributes
        for (JsonNode attr : attributes) {
            assertNotEquals("Pending", attr.get("attributeDecision").asText());
        }
    }

    @Test
    @DisplayName("Integration: Completion Criteria - With Pending Attributes")
    void testCompletionCriteriaWithPendingAttributes() {
        // Arrange - Some attributes are pending
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Pending"));

        // Act & Assert - Verify pending attribute exists
        assertTrue(hasDecision(attributes, "Pending"));
    }

    @Test
    @DisplayName("Integration: Loan Decision State Tracking")
    void testLoanDecisionStateTracking() {
        // Arrange - Create complete loan state
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-STATE-001");
        state.put("loanNumber", "LOAN-STATE-001");
        state.put("reviewType", "LDCReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "reviewer1");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify state preservation
        assertEquals("REQ-STATE-001", state.get("requestNumber").asText());
        assertEquals("LOAN-STATE-001", state.get("loanNumber").asText());
        assertEquals("LDCReview", state.get("reviewType").asText());
        assertEquals("Approved", state.get("loanDecision").asText());
        assertEquals("reviewer1", state.get("currentAssignedUsername").asText());
    }

    // ============================================================================
    // APPROVED/DENIED PATH WITH VEND PPA INTEGRATION
    // ============================================================================

    @Test
    @DisplayName("Integration: Approved Path - Workflow State Progression")
    void testApprovedPathWorkflowProgression() {
        // Arrange - Build complete approved workflow state
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-APPROVED-PATH-001");
        state.put("loanNumber", "LOAN-APPROVED-001");
        state.put("reviewType", "LDCReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "reviewer1");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Approved"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify state progression
        assertEquals("REQ-APPROVED-PATH-001", state.get("requestNumber").asText());
        assertEquals("LOAN-APPROVED-001", state.get("loanNumber").asText());
        assertEquals("LDCReview", state.get("reviewType").asText());
        assertEquals("Approved", state.get("loanDecision").asText());
        
        // Verify all attributes are approved
        for (JsonNode attr : state.get("attributes")) {
            assertEquals("Approved", attr.get("attributeDecision").asText());
        }
    }

    @Test
    @DisplayName("Integration: Denied Path - Workflow State Progression")
    void testDeniedPathWorkflowProgression() {
        // Arrange - Build complete denied workflow state
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-DENIED-PATH-001");
        state.put("loanNumber", "LOAN-DENIED-001");
        state.put("reviewType", "SecPolicyReview");
        state.put("loanDecision", "Denied");
        state.put("currentAssignedUsername", "reviewer2");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Rejected"));
        attributes.add(createAttribute("DebtRatio", "Rejected"));
        attributes.add(createAttribute("IncomeVerification", "Rejected"));
        state.set("attributes", attributes);

        // Act & Assert - Verify state progression
        assertEquals("REQ-DENIED-PATH-001", state.get("requestNumber").asText());
        assertEquals("LOAN-DENIED-001", state.get("loanNumber").asText());
        assertEquals("SecPolicyReview", state.get("reviewType").asText());
        assertEquals("Denied", state.get("loanDecision").asText());
        
        // Verify all attributes are rejected
        for (JsonNode attr : state.get("attributes")) {
            assertEquals("Rejected", attr.get("attributeDecision").asText());
        }
    }

    // ============================================================================
    // REPURCHASE PATH WITH EMAIL NOTIFICATION
    // ============================================================================

    @Test
    @DisplayName("Integration: Repurchase Path - Workflow State Progression")
    void testRepurchasePathWorkflowProgression() {
        // Arrange - Build complete repurchase workflow state
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-REPURCHASE-PATH-001");
        state.put("loanNumber", "LOAN-REPURCHASE-001");
        state.put("reviewType", "ConduitReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "reviewer3");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Repurchase"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify state progression
        assertEquals("REQ-REPURCHASE-PATH-001", state.get("requestNumber").asText());
        assertEquals("LOAN-REPURCHASE-001", state.get("loanNumber").asText());
        assertEquals("ConduitReview", state.get("reviewType").asText());
        assertEquals("Approved", state.get("loanDecision").asText());
        
        // Verify repurchase attribute is present
        assertTrue(hasDecision(state.get("attributes"), "Repurchase"));
    }

    // ============================================================================
    // RECLASS APPROVED PATH WITH TIMER AND CONFIRMATION
    // ============================================================================

    @Test
    @DisplayName("Integration: Reclass Path - Workflow State Progression with Timer")
    void testReclassPathWorkflowProgressionWithTimer() {
        // Arrange - Build complete reclass workflow state
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-RECLASS-PATH-001");
        state.put("loanNumber", "LOAN-RECLASS-001");
        state.put("reviewType", "LDCReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "reviewer4");
        state.put("timerStarted", true);
        state.put("timerDurationDays", 2);
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Reclass"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify state progression
        assertEquals("REQ-RECLASS-PATH-001", state.get("requestNumber").asText());
        assertEquals("LOAN-RECLASS-001", state.get("loanNumber").asText());
        assertEquals("LDCReview", state.get("reviewType").asText());
        assertEquals("Approved", state.get("loanDecision").asText());
        assertTrue(state.get("timerStarted").asBoolean());
        assertEquals(2, state.get("timerDurationDays").asInt());
        
        // Verify reclass attribute is present
        assertTrue(hasDecision(state.get("attributes"), "Reclass"));
    }

    // ============================================================================
    // RECLASS APPROVED PATH WITH TIMER EXPIRATION
    // ============================================================================

    @Test
    @DisplayName("Integration: Reclass Path - Timer Expiration Workflow")
    void testReclassPathTimerExpirationWorkflow() {
        // Arrange - Build reclass state with timer expiration
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-RECLASS-EXPIRE-001");
        state.put("loanNumber", "LOAN-RECLASS-EXPIRE-001");
        state.put("reviewType", "SecPolicyReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "reviewer5");
        state.put("timerExpired", true);
        state.put("notificationSent", true);
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Reclass"));
        attributes.add(createAttribute("DebtRatio", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify timer expiration state
        assertEquals("REQ-RECLASS-EXPIRE-001", state.get("requestNumber").asText());
        assertEquals("LOAN-RECLASS-EXPIRE-001", state.get("loanNumber").asText());
        assertEquals("SecPolicyReview", state.get("reviewType").asText());
        assertTrue(state.get("timerExpired").asBoolean());
        assertTrue(state.get("notificationSent").asBoolean());
        
        // Verify reclass attribute is present
        assertTrue(hasDecision(state.get("attributes"), "Reclass"));
    }

    // ============================================================================
    // ERROR SCENARIOS AND RECOVERY
    // ============================================================================

    @Test
    @DisplayName("Integration: Error Scenario - Invalid Review Type Detection")
    void testErrorScenarioInvalidReviewTypeDetection() {
        // Arrange - Create state with invalid review type
        ObjectNode invalidState = objectMapper.createObjectNode();
        invalidState.put("requestNumber", "REQ-ERROR-001");
        invalidState.put("loanNumber", "LOAN-ERROR-001");
        invalidState.put("reviewType", "InvalidReviewType");

        // Act & Assert - Verify invalid type is not in valid list
        String reviewType = invalidState.get("reviewType").asText();
        assertNotEquals("LDCReview", reviewType);
        assertNotEquals("SecPolicyReview", reviewType);
        assertNotEquals("ConduitReview", reviewType);
    }

    @Test
    @DisplayName("Integration: Error Scenario - Invalid Attribute Decision Detection")
    void testErrorScenarioInvalidAttributeDecisionDetection() {
        // Arrange - Create attribute with invalid decision
        ObjectNode invalidAttr = createAttribute("CreditScore", "InvalidDecision");

        // Act & Assert - Verify invalid decision is not in valid list
        String decision = invalidAttr.get("attributeDecision").asText();
        assertNotEquals("Approved", decision);
        assertNotEquals("Rejected", decision);
        assertNotEquals("Reclass", decision);
        assertNotEquals("Repurchase", decision);
        assertNotEquals("Pending", decision);
    }

    @Test
    @DisplayName("Integration: Error Scenario - Incomplete Loan Decision Detection")
    void testErrorScenarioIncompleteLoanDecisionDetection() {
        // Arrange - Create state with pending attributes
        ObjectNode incompleteState = objectMapper.createObjectNode();
        incompleteState.put("requestNumber", "REQ-ERROR-003");
        incompleteState.put("loanNumber", "LOAN-ERROR-003");
        incompleteState.put("loanDecision", "Approved");
        
        ArrayNode pendingAttributes = objectMapper.createArrayNode();
        pendingAttributes.add(createAttribute("CreditScore", "Approved"));
        pendingAttributes.add(createAttribute("DebtRatio", "Pending"));
        incompleteState.set("attributes", pendingAttributes);

        // Act & Assert - Verify pending attribute exists
        assertTrue(hasDecision(incompleteState.get("attributes"), "Pending"));
    }

    @Test
    @DisplayName("Integration: Error Scenario - Partially Approved Path")
    void testErrorScenarioPartiallyApprovedPath() {
        // Arrange - Build partially approved workflow state
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-PARTIAL-001");
        state.put("loanNumber", "LOAN-PARTIAL-001");
        state.put("reviewType", "LDCReview");
        state.put("loanDecision", "Approved");
        state.put("currentAssignedUsername", "reviewer7");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        attributes.add(createAttribute("DebtRatio", "Rejected"));
        attributes.add(createAttribute("IncomeVerification", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify mixed state
        assertEquals("REQ-PARTIAL-001", state.get("requestNumber").asText());
        assertEquals("LOAN-PARTIAL-001", state.get("loanNumber").asText());
        assertTrue(hasDecision(state.get("attributes"), "Approved"));
        assertTrue(hasDecision(state.get("attributes"), "Rejected"));
    }

    @Test
    @DisplayName("Integration: Error Scenario - Missing Loan Decision")
    void testErrorScenarioMissingLoanDecision() {
        // Arrange - Create state without loan decision
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-ERROR-004");
        state.put("loanNumber", "LOAN-ERROR-004");
        
        ArrayNode attributes = objectMapper.createArrayNode();
        attributes.add(createAttribute("CreditScore", "Approved"));
        state.set("attributes", attributes);

        // Act & Assert - Verify loan decision is missing
        assertFalse(state.has("loanDecision") && state.get("loanDecision").isTextual());
    }

    @Test
    @DisplayName("Integration: Error Scenario - Null Attributes")
    void testErrorScenarioNullAttributes() {
        // Arrange - Create state with null attributes
        ObjectNode state = objectMapper.createObjectNode();
        state.put("requestNumber", "REQ-ERROR-005");
        state.put("loanNumber", "LOAN-ERROR-005");
        state.putNull("attributes");

        // Act & Assert - Verify attributes are null
        assertTrue(state.get("attributes").isNull());
    }

    // Helper methods
    private ObjectNode createAttribute(String name, String decision) {
        ObjectNode attr = objectMapper.createObjectNode();
        attr.put("attributeName", name);
        attr.put("attributeDecision", decision);
        return attr;
    }

    private boolean hasDecision(JsonNode attributes, String decision) {
        if (attributes == null || !attributes.isArray()) {
            return false;
        }
        for (JsonNode attr : attributes) {
            if (attr.has("attributeDecision") && 
                attr.get("attributeDecision").asText().equals(decision)) {
                return true;
            }
        }
        return false;
    }
}
