package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailNotificationHandlerTest {

    private EmailNotificationHandler handler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new EmailNotificationHandler();
    }

    // Note: Testing sendEmail requires setting environment variables which is hard
    // in unit tests.
    // However, the handler reads System.getenv("SENDER_EMAIL").
    // If not set, it throws RuntimeException in sendEmail, caught in apply.
    // We can simulate success if we could mock System.getenv, but we can't easily.
    // So we test the failure path or structure mostly, OR we assume we can't fully
    // cover sendEmail success
    // without PowerMock or environment injection.
    // BUT the catch block returns success=false.

    @Test
    void testSuccessfulExecution() {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("notificationType", "Repurchase");
        input.put("loanNumber", "LOAN-123");
        input.put("requestNumber", "REQ-123");
        input.put("templateName", "template-1");

        JsonNode result = handler.apply(input);

        assertTrue(result.get("success").asBoolean());
        assertTrue(result.get("sent").asBoolean());
        assertTrue(result.get("messageId").asText().startsWith("mock-message-id-"));
    }
}
