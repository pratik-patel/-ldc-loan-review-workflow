package com.ldc.workflow;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AWS Lambda handler for LDC Loan Review Workflow.
 * 
 * This handler initializes Spring Boot context and routes requests to appropriate handlers.
 * It implements the AWS Lambda RequestHandler interface directly.
 */
public class LambdaHandler implements RequestHandler<JsonNode, JsonNode> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static ApplicationContext applicationContext;

    static {
        try {
            logger.info("Initializing Spring Boot application context");
            applicationContext = SpringApplication.run(LambdaApplication.class);
            logger.info("Spring Boot application context initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Spring Boot application context", e);
            throw new RuntimeException("Failed to initialize Spring Boot context", e);
        }
    }

    @Override
    public JsonNode handleRequest(JsonNode input, Context context) {
        try {
            logger.info("Lambda handler invoked with input: {}", input);
            
            // Get the loan review router bean from Spring context
            com.ldc.workflow.handlers.LoanReviewRouter router = 
                applicationContext.getBean(com.ldc.workflow.handlers.LoanReviewRouter.class);
            
            // Route the request to the appropriate handler
            JsonNode response = router.apply(input);
            
            logger.info("Lambda handler returning response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error processing Lambda request", e);
            return objectMapper.createObjectNode()
                .put("error", e.getMessage())
                .put("errorType", e.getClass().getSimpleName());
        }
    }
}
