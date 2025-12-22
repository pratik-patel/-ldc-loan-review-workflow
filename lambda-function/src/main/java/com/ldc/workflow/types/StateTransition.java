package com.ldc.workflow.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StateTransition {
    @JsonProperty("WorkflowStateName")
    private String workflowStateName;

    @JsonProperty("WorkflowStateUserId")
    private String workflowStateUserId;

    @JsonProperty("WorkflowStateStartDateTime")
    private String workflowStateStartDateTime;

    @JsonProperty("WorkflowStateEndDateTime")
    private String workflowStateEndDateTime;

    public StateTransition() {
    }

    public StateTransition(String workflowStateName, String workflowStateUserId, String workflowStateStartDateTime,
            String workflowStateEndDateTime) {
        this.workflowStateName = workflowStateName;
        this.workflowStateUserId = workflowStateUserId;
        this.workflowStateStartDateTime = workflowStateStartDateTime;
        this.workflowStateEndDateTime = workflowStateEndDateTime;
    }

    public String getWorkflowStateName() {
        return workflowStateName;
    }

    public void setWorkflowStateName(String workflowStateName) {
        this.workflowStateName = workflowStateName;
    }

    public String getWorkflowStateUserId() {
        return workflowStateUserId;
    }

    public void setWorkflowStateUserId(String workflowStateUserId) {
        this.workflowStateUserId = workflowStateUserId;
    }

    public String getWorkflowStateStartDateTime() {
        return workflowStateStartDateTime;
    }

    public void setWorkflowStateStartDateTime(String workflowStateStartDateTime) {
        this.workflowStateStartDateTime = workflowStateStartDateTime;
    }

    public String getWorkflowStateEndDateTime() {
        return workflowStateEndDateTime;
    }

    public void setWorkflowStateEndDateTime(String workflowStateEndDateTime) {
        this.workflowStateEndDateTime = workflowStateEndDateTime;
    }
}
