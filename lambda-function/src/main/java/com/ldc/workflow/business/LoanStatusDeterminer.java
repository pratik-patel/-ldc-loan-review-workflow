package com.ldc.workflow.business;

import com.ldc.workflow.types.LoanAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Determines the final loan status based on attribute decisions.
 * Implements the business logic for status determination.
 */
@Component
public class LoanStatusDeterminer {

    private static final Logger logger = LoggerFactory.getLogger(LoanStatusDeterminer.class);

    /**
     * Determine the loan status based on attribute decisions.
     * 
     * Rules:
     * - All Approved → "Approved"
     * - At least one Approved AND at least one Rejected → "Partially Approved"
     * - All Rejected → "Rejected"
     * - At least one Repurchase → "Repurchase" (TBD: final business rules)
     * - At least one Reclass → "Reclass Approved" (TBD: final business rules)
     */
    public String determineStatus(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            logger.warn("No attributes provided for status determination");
            return "Unknown";
        }

        // Count decisions
        int approvedCount = 0;
        int rejectedCount = 0;
        int reclassCount = 0;
        int repurchaseCount = 0;
        int pendingCount = 0;

        for (LoanAttribute attr : attributes) {
            String decision = attr.getAttributeDecision();
            if (decision == null || "Pending".equals(decision)) {
                pendingCount++;
            } else if ("Approved".equals(decision)) {
                approvedCount++;
            } else if ("Rejected".equals(decision)) {
                rejectedCount++;
            } else if ("Reclass".equals(decision)) {
                reclassCount++;
            } else if ("Repurchase".equals(decision)) {
                repurchaseCount++;
            }
        }

        logger.debug("Status determination: approved={}, rejected={}, reclass={}, repurchase={}, pending={}", 
                approvedCount, rejectedCount, reclassCount, repurchaseCount, pendingCount);

        // Determine status based on rules
        // TBD: Repurchase and Reclass rules - using simple priority for now
        if (repurchaseCount > 0) {
            logger.info("Loan status determined as: Repurchase (TBD: final business rules)");
            return "Repurchase";
        }

        if (reclassCount > 0) {
            logger.info("Loan status determined as: Reclass Approved (TBD: final business rules)");
            return "Reclass Approved";
        }

        if (approvedCount > 0 && rejectedCount > 0) {
            logger.info("Loan status determined as: Partially Approved");
            return "Partially Approved";
        }

        if (approvedCount > 0 && rejectedCount == 0 && reclassCount == 0 && repurchaseCount == 0) {
            logger.info("Loan status determined as: Approved");
            return "Approved";
        }

        if (rejectedCount > 0 && approvedCount == 0 && reclassCount == 0 && repurchaseCount == 0) {
            logger.info("Loan status determined as: Rejected");
            return "Rejected";
        }

        logger.warn("Unable to determine loan status from attributes");
        return "Unknown";
    }

    /**
     * Check if all attributes are approved.
     */
    public boolean areAllApproved(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        return attributes.stream()
                .allMatch(attr -> "Approved".equals(attr.getAttributeDecision()));
    }

    /**
     * Check if all attributes are rejected.
     */
    public boolean areAllRejected(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        return attributes.stream()
                .allMatch(attr -> "Rejected".equals(attr.getAttributeDecision()));
    }

    /**
     * Check if at least one attribute is approved and at least one is rejected.
     */
    public boolean hasApprovedAndRejected(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        boolean hasApproved = attributes.stream()
                .anyMatch(attr -> "Approved".equals(attr.getAttributeDecision()));
        boolean hasRejected = attributes.stream()
                .anyMatch(attr -> "Rejected".equals(attr.getAttributeDecision()));
        return hasApproved && hasRejected;
    }

    /**
     * Check if at least one attribute has repurchase decision.
     */
    public boolean hasRepurchase(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        return attributes.stream()
                .anyMatch(attr -> "Repurchase".equals(attr.getAttributeDecision()));
    }

    /**
     * Check if at least one attribute has reclass decision.
     */
    public boolean hasReclass(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        return attributes.stream()
                .anyMatch(attr -> "Reclass".equals(attr.getAttributeDecision()));
    }
}
