package com.bonyan.rtd.service;

import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;

public class ChunkWriter {

    private final EventRecordService eventRecordService;

    public ChunkWriter(EventRecordService eventRecordService) {
        this.eventRecordService = eventRecordService;
    }

    public void writeRecord(EventRecord eventRecord) {
        this.eventRecordService.write("OUT", eventRecord);
    }
}
