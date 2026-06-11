package com.bonyan.rtd.entity;


import java.util.Objects;
import java.util.UUID;

public class RtdAction<T> {
    private String requestId;
    private T actionId;
    private T contentId;
    private String actionMessageContent;


    public RtdAction() {
    }

    public RtdAction(T contentId) {
        generateNewId();
        this.contentId = contentId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void generateNewId() {
        this.requestId = UUID.randomUUID().toString();
    }

    public T getActionId() {
        return actionId;
    }

    public void setActionId(T actionId) {
        this.actionId = actionId;
    }

    public T getContentId() {
        return contentId;
    }

    public void setContentId(T contentId) {
        this.contentId = contentId;
    }

    public String getActionMessageContent() {
        return actionMessageContent;
    }

    public void setActionMessageContent(String actionMessageContent) {
        this.actionMessageContent = actionMessageContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RtdAction<T> rtdAction = (RtdAction<T>) o;
        return Objects.equals(contentId, rtdAction.contentId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(contentId);
    }
}

