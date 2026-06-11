package com.bonyan.rtd.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkRepository<T> extends HashMap<T, Chunk<T>> {


    public int addRecord(RtdAction<T> rtdAction, Map.Entry<String, Integer> msisdn) {
        this.get(rtdAction.getContentId()).setRtdAction(rtdAction);
        return this.get(rtdAction.getContentId()).addRecord(msisdn);
    }

    @Override
    public Chunk<T> get(Object key) {
        if (super.get(key) == null) this.addChunk((T) key);
        return super.get(key);
    }

    public void addChunk(T contentId) {
        Chunk<T> chunk = new Chunk<>(contentId);
        this.put(contentId, chunk);
    }

    public Set<Chunk<T>> getUntouchedChunk() {
        Set<Chunk<T>> untouchedChunk = new HashSet<>();
        for (Chunk<T> chunk : this.values()) {
            if (!chunk.isTouched()) {
                untouchedChunk.add(chunk);
            }
        }
        return untouchedChunk;
    }

    public void resetTouched() {
        for (Chunk<T> chunk : this.values()) {
            chunk.setTouched(false);
        }
    }
}
