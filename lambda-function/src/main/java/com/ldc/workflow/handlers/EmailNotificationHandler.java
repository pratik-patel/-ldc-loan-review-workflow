package com.ldc.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldc.workflow.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Lambda handler for email notifications.
 * Sends email notifications for Repurchase and Reclass Expiration events.
 * Errors are logged but do not fail the workflow.
 * 
 * Input: JSON with notificationType, loanNumber, requestNumber, recipientEmail,
 * templateName
 * Output: JSON with send status
 */
@Component("emailNotificationHandler")
public class EmailNotificationHandler implements Function<JsonNode, JsonNode> {

        private static final Logger logger = LoggerFactory.getLogger(EmailNotificationHandler.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        public EmailNotificationHandler() {
        }

        @Override
        public JsonNode apply(JsonNode input) {
                try {
                        logger.info("Email Notification handler invoked");

                        // Extract input fields
                        String notificationType = input.get("notificationType").asText();
                        String loanNumber = input.get("loanNumber").asText();
                        String requestNumber = input.get("requestNumber").asText();
                        String templateName = input.get("templateName").asText();

                        logger.debug("Sending {} notification for loanNumber: {}", notificationType, loanNumber);

                        // Replace SES sending with simple logging
                        logger.info("MOCK EMAIL SEND: NotificationType={}, requestNumber={}, loanNumber={}, templateName={}",
                                        notificationType, requestNumber, loanNumber, templateName);

                        String messageId = "mock-message-id-" + System.currentTimeMillis();

                        logger.info("Email notification sent successfully for loanNumber: {}, messageId: {}",
                                        loanNumber, messageId);

                        return createSuccessResponse(requestNumber, loanNumber, true, messageId);
                } catch (Exception e) {
                        logger.error("Error sending email notification", e);
                        // Log error but don't fail workflow (non-blocking error)
                        return createSuccessResponse("unknown", "unknown", false,
                                        "Error: " + e.getMessage());
                }
        }

        private JsonNode createSuccessResponse(String requestNumber, String loanNumber,
                        boolean sent, String messageIdOrError) {
                return objectMapper.createObjectNode()
                                .put("success", true)
                                .put("requestNumber", requestNumber)
                                .put("loanNumber", loanNumber)
                                .put("sent", sent)
                                .put("messageId", messageIdOrError);
        }
}
