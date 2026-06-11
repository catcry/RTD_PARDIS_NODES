package com.bonyan.pardis;

public class PardisInputFileEntity {

    public static final String FIELD = "Field";
    public static final String ACTION_ID = "action_id";
    public static final String CONTENT_ID = "content_id";
    public static final String MSISDN = "msisdn";
    public static final String RETRY_COUNT = "retry_count";
    public static final String REQUIRED_MSG = " is required";
    public static final String FIELD_PREFIX = "F ";
    public static final String BLOCK_PREFIX = "B ";

    private String recordPreText;
    private String recordPostText;
    private String actionId;
    private String contentId;
    private String msisdn;
    private String retryCount;


    public PardisInputFileEntity() {
        this.recordPreText = "RECORD\n" +
                "#input_id 1721052846x005_00150\n" +
                "#output_id \n" +
                "#input_type \n" +
                "#output_type \n" +
                "#addkey \n" +
                "#source_id RTD1_ACTIONS\n" +
                "#filename test.OUT.collected\n";
        this.recordPostText = ".\n";
    }

    public PardisInputFileEntity(String actionId, String contentId, String msisdn, String retryCount) {
        this();
        this.actionId = actionId;
        this.contentId = contentId;
        this.msisdn = msisdn;
        this.retryCount = retryCount;
    }

    public String getActionId() {
        return actionId;
    }

    public PardisInputFileEntity setActionId(String actionId) {
        this.actionId = actionId;
        return this;
    }

    public String getContentId() {
        return contentId;
    }

    public PardisInputFileEntity setContentId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public PardisInputFileEntity setMsisdn(String msisdn) {
        this.msisdn = msisdn;
        return this;
    }

    public String getRetryCount() {
        return retryCount;
    }

    public PardisInputFileEntity setRetryCount(String retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public String getRecordContent() throws IllegalArgumentException {
        checkValidity();
        return recordPreText +
                FIELD_PREFIX + ACTION_ID + " " + actionId + "\n" +
                FIELD_PREFIX + CONTENT_ID + " " + contentId + "\n" +
                FIELD_PREFIX + MSISDN + " " + msisdn + "\n" +
                FIELD_PREFIX + RETRY_COUNT + " " + retryCount + "\n" +
                recordPostText;
    }

    public void checkValidity() throws IllegalArgumentException {
        if (contentId == null || contentId.isEmpty()) {
            throw new IllegalArgumentException(CONTENT_ID + REQUIRED_MSG);
        }
        if (msisdn == null || msisdn.isEmpty()) {
            throw new IllegalArgumentException(MSISDN + REQUIRED_MSG);
        }
        if (retryCount == null || retryCount.isEmpty()) {
            throw new IllegalArgumentException(RETRY_COUNT + REQUIRED_MSG);
        }
    }
}
