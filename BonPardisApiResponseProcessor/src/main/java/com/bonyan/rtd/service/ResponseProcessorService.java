package com.bonyan.rtd.service;

import com.bonyan.rtd.entity.Chunk;
import com.bonyan.rtd.utils.JsonParser;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.NodeContext;
import com.bonyan.rtd.PardisResponseProcessorNode;


import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class ResponseProcessorService extends Nodebase {
    public static final String MSISDN = "msisdn";
    public static final String RETRY_COUNT = "retry_count";
    public static final String TYPE = "type";
    public static final String FAILED = "FAILED";
    public static final String OUT_PARDIS = "OUT_PARDIS";
    public static final String SUCCEED = "SUCCEED";

    private final EventRecordService eventRecordService;
    private final PardisResponseProcessorNode.NodeParameters nodeParameters;
    private final NodeContext context;


    public ResponseProcessorService(EventRecordService eventRecordService, NodeContext context, PardisResponseProcessorNode.NodeParameters nodeParams) {
        this.eventRecordService = eventRecordService;
        this.context = context;
        this.nodeParameters = nodeParams;
    }

    public static boolean isNumeric(String input) {
        return input.matches("-?\\d+");
    }

    public void processResponseRecord() {
        Chunk<String> chunk = new Chunk<>();
        i_begin();

        i_next("Action");
        i_enter();
        String actionId = i_get("action_id");
        String contentId = i_get("content_id");
        chunk.getRtdAction().setActionId(actionId);
        chunk.setContentId(contentId);
        chunk.getRtdAction().setContentId(contentId);
        i_exit();

        i_next("ChunkList");
        i_enter();
        for (int i = 1; i_next("Sub_" + i) > 0; i++) {
            i_enter();
            String msisdn = i_get(MSISDN);
            Integer retryCount = Integer.valueOf(i_get(RETRY_COUNT));
            chunk.getRecords().add(new AbstractMap.SimpleEntry<>(msisdn, retryCount));
            i_exit();
        }
        i_exit();

        i_next("Response");
        i_enter();
        if (i_field_exists("Status") > 0) {
            Integer status = Integer.valueOf(i_get("Status"));
            String body = i_get("Body");
            chunk.setStatus(status);
            chunk.setErrorMessage(body);
            if (status.equals(200)) {
                List<String> smsIds = JsonParser.getJsonValues(body);
                chunk.setSmsIds(smsIds);
                String responseBody = i_get("ResponseBody");
                chunk.setResponseBody(responseBody);
            }
        } else {
            i_reject("REJECTED", "invalid input");
            context.writeMessage("REJECT_RECORD", "There is no status field in response record.");
        }
        i_exit();
        i_end();
        buildNewRecords(chunk);
    }

    public void buildNewRecords(Chunk<String> chunk) {
        EventRecord tempRecord = eventRecordService.newRecord();
        tempRecord.setOutputType("Pardis_Response");
        tempRecord.addField("action_id", chunk.getRtdAction().getActionId());
        tempRecord.addField("content_id", chunk.getRtdAction().getContentId());

        if (chunk.getStatus() == null || !chunk.getStatus().equals(200)) {
            handleFailedStatus(tempRecord, chunk);
        } else {
            if (chunk.getSmsIds().size() != chunk.getRecords().size()) {
                String errMsg = "Different size of sms ids and chunk list, smsIds size: "
                        + chunk.getSmsIds().size() + ", chunk list size: " + chunk.getRecords().size();
                i_reject("REJECTED", errMsg);
                tempRecord.markAsRejected();
                context.writeMessage("REJECTED", errMsg);
                return;
            }
            handleSuccessStatus(tempRecord, chunk);
        }
    }

    private void handleFailedStatus(EventRecord tempRecord, Chunk<String> chunk) {
        tempRecord.addField("error_msg", chunk.getErrorMessage());
        tempRecord.addField("error_status", chunk.getStatus()==null? "NO_STATUS": chunk.getStatus().toString());
        Integer maxRetryCount = nodeParameters.getMaxSendRetryCount();
        //Integer maxRetryCount = context.getParameterInt("max-retry-count");;
        for (Map.Entry<String, Integer> msisdnPair : chunk.getRecords()) {
            EventRecord newRecord = (EventRecord) tempRecord.copy();
            newRecord.addField(MSISDN, msisdnPair.getKey());
            newRecord.addField(RETRY_COUNT, msisdnPair.getValue().toString());
            Integer retryCount = msisdnPair.getValue();
            if (retryCount >= maxRetryCount){
                newRecord.addField(TYPE, FAILED);
                eventRecordService.write(FAILED, newRecord);
            }
            else {
                newRecord.addField(TYPE, OUT_PARDIS);
                eventRecordService.write(OUT_PARDIS, newRecord);
            }

        }
    }

    private void handleSuccessStatus(EventRecord tempRecord, Chunk<String> chunk) {
        Integer maxRetryCount = nodeParameters.getMaxSendRetryCount();
        //int maxRetryCount = context.getParameterInt("max-retry-count");
        for (int i = 0; i < chunk.getSmsIds().size(); i++) {
            String smsId = chunk.getSmsIds().get(i);
            Map.Entry<String, Integer> msisdnPair = chunk.getRecords().get(i);
            EventRecord newRecord = (EventRecord) tempRecord.copy();
            newRecord.addField(MSISDN, msisdnPair.getKey());
            newRecord.addField(RETRY_COUNT, msisdnPair.getValue().toString());
            newRecord.addField("sms_id", smsId);
            int retryCount = msisdnPair.getValue();
            int errorCode = -1;
            if (isNumeric(smsId)) {
                errorCode = Integer.parseInt(smsId);
            }
            if (errorCode > 0 && errorCode < 255) {
                newRecord.addField("error_msg", "Pardis API could not send SMS to this MSISDN");
                if (retryCount >= maxRetryCount){
                    newRecord.addField(TYPE, FAILED);
                    eventRecordService.write(FAILED, newRecord);
                }
                else {
                    newRecord.addField(TYPE, OUT_PARDIS);
                    eventRecordService.write(OUT_PARDIS, newRecord);
                }

            } else {
                newRecord.addField(TYPE, SUCCEED);
                eventRecordService.write(SUCCEED, newRecord);
            }
        }
    }

}
