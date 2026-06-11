package com.bonyan.rtd.entity;

import java.util.Map;

public class Chunk<T> {
    private T actionId;
    private RtdAction<T> rtdAction;
    private RecordList records = new RecordList();
    private boolean touched;

    public Chunk(T contentId) {
        this.rtdAction = new RtdAction<>(contentId);
        this.actionId = contentId;
    }

    public int addRecord(Map.Entry<String, Integer> msisdn) {
        records.add(msisdn);
        setTouched(true);
        return records.size();
    }


    public T getActionId() {
        return actionId;
    }

    public void setActionId(T actionId) {
        this.actionId = actionId;
    }

    public RtdAction<T> getRtdAction() {
        return rtdAction;
    }

    public void setRtdAction(RtdAction<T> rtdAction) {
        this.rtdAction = rtdAction;
    }

    public RecordList getRecords() {
        return records;
    }

    public void setRecords(RecordList records) {
        this.records = records;
    }

    public boolean isTouched() {
        return touched;
    }

    public void setTouched(boolean touched) {
        this.touched = touched;
    }
}
