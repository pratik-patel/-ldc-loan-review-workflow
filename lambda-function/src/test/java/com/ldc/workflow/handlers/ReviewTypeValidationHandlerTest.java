package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ldc.workflow.validation.ReviewTypeValidator;
import com.ldc.workflow.repository.WorkflowStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReviewTypeValidationHandler
 * Tests validation of review types and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewTypeValidationHandler Tests")
class ReviewTypeValidationHandlerTest {

    private ObjectMapper objectMapper;
    private ReviewTypeValidationHandler handler;

    @Mock
    private ReviewTypeValidator reviewTypeValidator;

    @Mock
    private WorkflowStateRepository workflowStateRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new ReviewTypeValidationHandler(reviewTypeValidator, workflowStateRepository);

        // Mock repository save to avoid DynamoDB calls
        org.mockito.Mockito.lenient().doNothing().when(workflowStateRepository).save(any());

        // Mock validator methods with lenient() to avoid unnecessary stubbing errors
        org.mockito.Mockito.lenient().when(reviewTypeValidator.getAllowedReviewTypes())
                .thenReturn(java.util.Set.of("LDCReview", "SecPolicyReview", "ConduitReview"));
        org.mockito.Mockito.lenient().when(reviewTypeValidator.getErrorMessage(anyString()))
                .thenReturn("Invalid review type");
        // Default to false for isValid - tests will override as needed
        org.mockito.Mockito.lenient().when(reviewTypeValidator.isValid(anyString()))
                .thenReturn(false);
    }

    @Test
    @DisplayName("Should validate LDCReview type successfully")
    void testValidateLDCReviewType() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-001");
        input.put("loanNumber", "LOAN-001");
        input.put("reviewType", "LDCReview");
        input.put("currentAssignedUsername", "testuser");

        when(reviewTypeValidator.isValid("LDCReview")).thenReturn(true);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("LDCReview", result.get("reviewType").asText());
    }

    @Test
    @DisplayName("Should validate SecPolicyReview type successfully")
    void testValidateSecPolicyReviewType() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-002");
        input.put("loanNumber", "LOAN-002");
        input.put("reviewType", "SecPolicyReview");
        input.put("currentAssignedUsername", "testuser");

        when(reviewTypeValidator.isValid("SecPolicyReview")).thenReturn(true);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("SecPolicyReview", result.get("reviewType").asText());
    }

    @Test
    @DisplayName("Should validate ConduitReview type successfully")
    void testValidateConduitReviewType() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-003");
        input.put("loanNumber", "LOAN-003");
        input.put("reviewType", "ConduitReview");
        input.put("currentAssignedUsername", "testuser");

        when(reviewTypeValidator.isValid("ConduitReview")).thenReturn(true);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("ConduitReview", result.get("reviewType").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = { "InvalidType", "UnknownReview", "BadReview", "", "null" })
    @DisplayName("Should reject invalid review types")
    void testRejectInvalidReviewTypes(String invalidType) {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-INVALID");
        input.put("loanNumber", "LOAN-INVALID");
        input.put("reviewType", invalidType);
        input.put("currentAssignedUsername", "testuser");

        when(reviewTypeValidator.isValid(invalidType)).thenReturn(false);
        when(reviewTypeValidator.getErrorMessage(invalidType))
                .thenReturn("Invalid review type: " + invalidType);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.has("error"));
    }

    @Test
    @DisplayName("Should handle missing reviewType field gracefully")
    void testMissingReviewTypeField() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-MISSING");
        input.put("loanNumber", "LOAN-MISSING");
        input.put("currentAssignedUsername", "testuser");

        // Act
        JsonNode result = handler.apply(input);

        // Debug
        if (result == null) {
            System.out.println("DEBUG: result is null in testMissingReviewTypeField");
        } else {
            System.out.println("DEBUG: result is " + result.toString());
        }

        // Assert - should return error response
        assertNotNull(result, "Handler response should not be null");
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.has("error"));
    }

    @Test
    @DisplayName("Should handle missing requestNumber field gracefully")
    void testMissingRequestNumber() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("loanNumber", "LOAN-001");
        input.put("reviewType", "LDCReview");
        input.put("currentAssignedUsername", "testuser");

        // Act
        JsonNode result = handler.apply(input);

        // Assert - should return error response
        assertNotNull(result, "Handler response should not be null");
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.has("error"));
    }

    @Test
    @DisplayName("Should handle missing loanNumber field gracefully")
    void testMissingLoanNumber() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-001");
        input.put("reviewType", "LDCReview");
        input.put("currentAssignedUsername", "testuser");

        // Act
        JsonNode result = handler.apply(input);

        // Assert - should return error response
        assertNotNull(result, "Handler response should not be null");
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.has("error"));
    }

    @Test
    @DisplayName("Should preserve requestNumber in response")
    void testPreserveRequestNumber() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-PRESERVE-001");
        input.put("loanNumber", "LOAN-001");
        input.put("reviewType", "LDCReview");
        input.put("currentAssignedUsername", "testuser");

        // Fix: Mock valid review type so we get a success response
        when(reviewTypeValidator.isValid("LDCReview")).thenReturn(true);

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertEquals("REQ-PRESERVE-001", result.get("requestNumber").asText());
    }

    @Test
    @DisplayName("Should preserve loanNumber in response")
    void testPreserveLoanNumber() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-001");
        input.put("loanNumber", "LOAN-PRESERVE-001");
        input.put("reviewType", "LDCReview");
        input.put("currentAssignedUsername", "testuser");

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertEquals("LOAN-PRESERVE-001", result.get("loanNumber").asText());
    }

    @Test
    @DisplayName("Should handle case-sensitive review types")
    void testCaseSensitiveReviewTypes() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-CASE");
        input.put("loanNumber", "LOAN-CASE");
        input.put("reviewType", "ldcreview"); // lowercase
        input.put("currentAssignedUsername", "testuser");

        when(reviewTypeValidator.isValid("ldcreview")).thenReturn(false);
        when(reviewTypeValidator.getErrorMessage("ldcreview"))
                .thenReturn("Invalid review type: ldcreview");

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
    }

    @Test
    @DisplayName("Should handle whitespace in review types")
    void testWhitespaceInReviewTypes() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("requestNumber", "REQ-SPACE");
        input.put("loanNumber", "LOAN-SPACE");
        input.put("reviewType", " LDCReview "); // with spaces
        input.put("currentAssignedUsername", "testuser");

        when(reviewTypeValidator.isValid(" LDCReview ")).thenReturn(false);
        when(reviewTypeValidator.getErrorMessage(" LDCReview "))
                .thenReturn("Invalid review type:  LDCReview ");

        // Act
        JsonNode result = handler.apply(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
    }
}
