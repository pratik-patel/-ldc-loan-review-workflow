package com.ldc.workflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for retrieving configuration from AWS Systems Manager Parameter Store.
 * Caches parameters to reduce API calls.
 */
@Service
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    private final SsmClient ssmClient;
    private final Map<String, String> parameterCache = new HashMap<>();

    public ConfigurationService(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

    /**
     * Get email template from Parameter Store.
     * Caches the result to avoid repeated API calls.
     */
    public String getEmailTemplate(String templateName) {
        String parameterName = "/ldc-workflow/email-templates/" + templateName;
        return getParameter(parameterName);
    }

    /**
     * Get notification email address from Parameter Store.
     */
    public String getNotificationEmail(String notificationType) {
        String parameterName = "/ldc-workflow/notifications/" + notificationType + "-email";
        return getParameter(parameterName);
    }

    /**
     * Get Vend PPA endpoint from Parameter Store.
     */
    public String getVendPpaEndpoint() {
        return getParameter("/ldc-workflow/vend-ppa/endpoint");
    }

    /**
     * Get any parameter from Parameter Store with caching.
     */
    public String getParameter(String parameterName) {
        try {
            // Check cache first
            if (parameterCache.containsKey(parameterName)) {
                logger.debug("Retrieved parameter from cache: {}", parameterName);
                return parameterCache.get(parameterName);
            }

            // Retrieve from Parameter Store
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            String value = response.parameter().value();

            // Cache the result
            parameterCache.put(parameterName, value);
            logger.debug("Retrieved parameter from Parameter Store: {}", parameterName);

            return value;
        } catch (Exception e) {
            // Check if it's a ParameterNotFound error
            if (e.getMessage() != null && e.getMessage().contains("ParameterNotFound")) {
                logger.error("Parameter not found in Parameter Store: {}", parameterName);
                throw new RuntimeException("Configuration parameter not found: " + parameterName, e);
            }
            logger.error("Error retrieving parameter from Parameter Store: {}", parameterName, e);
            throw new RuntimeException("Failed to retrieve configuration parameter: " + parameterName, e);
        }
    }

    /**
     * Clear the parameter cache (useful for testing or forcing refresh).
     */
    public void clearCache() {
        parameterCache.clear();
        logger.debug("Parameter cache cleared");
    }
}
