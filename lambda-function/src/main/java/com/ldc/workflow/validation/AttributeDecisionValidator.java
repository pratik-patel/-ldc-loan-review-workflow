package com.ldc.workflow.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator for attribute decision values.
 * Ensures attribute decisions are one of the allowed values.
 */
@Component
public class AttributeDecisionValidator {

    private static final Logger logger = LoggerFactory.getLogger(AttributeDecisionValidator.class);
    private static final Set<String> ALLOWED_DECISIONS = new HashSet<>(Arrays.asList(
            "Approved",
            "Rejected",
            "Reclass",
            "Repurchase",
            "Pending"
    ));

    /**
     * Validate that the attribute decision is one of the allowed values.
     * Null is also considered valid (represents unset decision).
     */
    public boolean isValid(String decision) {
        if (decision == null) {
            return true; // Null is valid (unset decision)
        }

        if (decision.trim().isEmpty()) {
            logger.warn("Attribute decision is empty string");
            return false;
        }

        boolean valid = ALLOWED_DECISIONS.contains(decision);
        if (!valid) {
            logger.warn("Invalid attribute decision: {}. Allowed values: {}", decision, ALLOWED_DECISIONS);
        }
        return valid;
    }

    /**
     * Get the set of allowed decisions.
     */
    public Set<String> getAllowedDecisions() {
        return new HashSet<>(ALLOWED_DECISIONS);
    }

    /**
     * Get error message for invalid decision.
     */
    public String getErrorMessage(String decision) {
        return String.format("Invalid attribute decision: '%s'. Must be one of: %s", 
                decision, String.join(", ", ALLOWED_DECISIONS));
    }

    /**
     * Check if a decision is "Pending" or null.
     */
    public boolean isPendingOrNull(String decision) {
        return decision == null || "Pending".equals(decision);
    }

    /**
     * Check if a decision is not "Pending" and not null.
     */
    public boolean isNotPendingAndNotNull(String decision) {
        return decision != null && !"Pending".equals(decision);
    }
}
