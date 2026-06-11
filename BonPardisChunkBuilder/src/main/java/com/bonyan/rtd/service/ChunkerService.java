package com.bonyan.rtd.service;

import com.bonyan.rtd.PardisChunkBuilderNode;
import com.bonyan.rtd.entity.Chunk;
import com.bonyan.rtd.entity.ChunkRepository;
import com.bonyan.rtd.entity.RecordList;
import com.bonyan.rtd.entity.RtdAction;
import com.comptel.eventlink.core.Nodebase;
import com.bonyan.rtd.utils.NodeRecordUtil;
import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.Field;
import java.text.DateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class ChunkerService {

    private final ChunkWriter chunkWriter;
    private final EventRecordService eventRecordService;
    private final ChunkRepository<String> chunkRepository;
    private final PardisChunkBuilderNode.NodeParameters nodeParameters;
    private Integer recordNumber;


    public ChunkerService(EventRecordService eventRecordService, PardisChunkBuilderNode.NodeParameters nodeParameters) {
        this.eventRecordService = eventRecordService;
        this.nodeParameters = nodeParameters;
        this.chunkWriter = new ChunkWriter(eventRecordService);
        this.chunkRepository = new ChunkRepository<>();
        this.recordNumber = 0;
    }

    public void writeChunk(String contentId, RecordList chunkList) {
        EventRecord eventRecord = this.eventRecordService.newRecord();

        Chunk<String> chunk = chunkRepository.get(contentId);
        buildChunk(chunk.getRtdAction(), chunkList, eventRecord);

        chunkWriter.writeRecord(eventRecord);
    }

    public void buildChunk(RtdAction<String> action, RecordList msisdnList, EventRecord eventRecord) {
        eventRecord.addField("record_generate_time", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date()));
        eventRecord.addField("chunk_id", action.getRequestId());
        Field actionBlock = eventRecord.addField("Action");
        actionBlock.addField("action_id", action.getActionId());
        actionBlock.addField("content_id", action.getContentId());

        int i = 1;
        Field chunkListBlock = eventRecord.addField("ChunkList");
        for (Map.Entry<String, Integer> msisdnPair : msisdnList) {
            Field subListBlock = chunkListBlock.addField("Sub_" + i);
            subListBlock.addField("msisdn", msisdnPair.getKey());
            subListBlock.addField("retry_count", msisdnPair.getValue().toString());
            i++;
        }

    }

    public void writeChunkRecords() {
        for (Map.Entry<String,Chunk<String>> entry : chunkRepository.entrySet()) {
            writeChunk(entry.getKey(), entry.getValue().getRecords());
        }
        removeUsedChunk(null);
        Nodebase.nb_pre_commit();
        Nodebase.nb_request_commit();
        Nodebase.nb_post_commit();
    }

    public void writeChunkRecord(RtdAction<String> action) {
        writeChunk(action.getContentId(), chunkRepository.get(action.getContentId()).getRecords());
        removeUsedChunk(action);
    }

    public void removeUsedChunk(RtdAction<String> action) {
        if (action != null) {
            chunkRepository.remove(action.getContentId());
        } else {
            chunkRepository.clear();
        }
    }

    public void addRecord(EventRecord eventRecord) {
        recordNumber++;
        writeUntouched();

        Integer retryCount = NodeRecordUtil.getFieldIntegerValue(eventRecord, "retry_count");
        String msisdn = NodeRecordUtil.getFieldStringValue(eventRecord, "msisdn");
        if (retryCount < nodeParameters.getMaxSendRetryCount()) {
            String actionId = NodeRecordUtil.getFieldStringValue(eventRecord, "action_id");
            String contentId = NodeRecordUtil.getFieldStringValue(eventRecord, "content_id");

            RtdAction<String> rtdAction = new RtdAction<>(contentId);
            rtdAction.setActionId(actionId);
            Map.Entry<String, Integer> msisdnPair = new AbstractMap.SimpleEntry<>(msisdn, retryCount + 1);
            int chunkSize = chunkRepository.addRecord(rtdAction, msisdnPair);

            if (nodeParameters.getMaxChunkSizeParam().equals(chunkSize)) {
                writeChunkRecord(rtdAction);
            }
        } else {
            eventRecord.reject("REJECTED", "Message for '" + msisdn + "' has been sent "
                    + nodeParameters.getMaxSendRetryCount() + " times.");
        }
    }

    public void writeUntouched() {
        if (recordNumber >= nodeParameters.getMaxUntouchedRecordCount()) {
            writeUntouchedChunk();
            recordNumber = 0;
        }
    }

    public void writeUntouchedChunk() {
        Set<Chunk<String>> untouchedChunk = chunkRepository.getUntouchedChunk();
        for (Chunk<String> chunk : untouchedChunk) {
            writeChunkRecord(chunk.getRtdAction());
        }
        chunkRepository.resetTouched();
    }
}
