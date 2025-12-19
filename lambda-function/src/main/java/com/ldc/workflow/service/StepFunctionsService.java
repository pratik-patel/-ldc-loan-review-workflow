package com.ldc.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service for interacting with AWS Step Functions API.
 * Uses HTTP client to call Step Functions SendTaskSuccess API.
 */
@Service
public class StepFunctionsService {

    private static final Logger logger = LoggerFactory.getLogger(StepFunctionsService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STEP_FUNCTIONS_ENDPOINT = "https://states.%s.amazonaws.com/";
    private static final String AWS_REGION = System.getenv("AWS_REGION");

    private final HttpClient httpClient;

    public StepFunctionsService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Send task success to Step Functions to resume execution.
     */
    public void sendTaskSuccess(String taskToken, String output) {
        try {
            String region = AWS_REGION != null ? AWS_REGION : "us-east-1";
            String endpoint = String.format(STEP_FUNCTIONS_ENDPOINT, region);

            // Create request body
            String requestBody = objectMapper.writeValueAsString(new SendTaskSuccessRequest(taskToken, output));

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-amz-json-1.0")
                    .header("X-Amz-Target", "AWSStepFunctions.SendTaskSuccess")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Step Functions SendTaskSuccess failed with status: {}, body: {}", 
                        response.statusCode(), response.body());
                throw new RuntimeException("Step Functions API call failed: " + response.statusCode());
            }

            logger.info("Task success sent to Step Functions");
        } catch (Exception e) {
            logger.error("Error sending task success to Step Functions", e);
            throw new RuntimeException("Failed to send task success to Step Functions", e);
        }
    }

    /**
     * Send task failure to Step Functions to fail execution.
     */
    public void sendTaskFailure(String taskToken, String error, String cause) {
        try {
            String region = AWS_REGION != null ? AWS_REGION : "us-east-1";
            String endpoint = String.format(STEP_FUNCTIONS_ENDPOINT, region);

            // Create request body
            String requestBody = objectMapper.writeValueAsString(
                    new SendTaskFailureRequest(taskToken, error, cause));

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-amz-json-1.0")
                    .header("X-Amz-Target", "AWSStepFunctions.SendTaskFailure")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Step Functions SendTaskFailure failed with status: {}, body: {}", 
                        response.statusCode(), response.body());
                throw new RuntimeException("Step Functions API call failed: " + response.statusCode());
            }

            logger.info("Task failure sent to Step Functions");
        } catch (Exception e) {
            logger.error("Error sending task failure to Step Functions", e);
            throw new RuntimeException("Failed to send task failure to Step Functions", e);
        }
    }

    /**
     * Request body for SendTaskSuccess API.
     */
    public static class SendTaskSuccessRequest {
        public String taskToken;
        public String output;

        public SendTaskSuccessRequest(String taskToken, String output) {
            this.taskToken = taskToken;
            this.output = output;
        }
    }

    /**
     * Request body for SendTaskFailure API.
     */
    public static class SendTaskFailureRequest {
        public String taskToken;
        public String error;
        public String cause;

        public SendTaskFailureRequest(String taskToken, String error, String cause) {
            this.taskToken = taskToken;
            this.error = error;
            this.cause = cause;
        }
    }
}
