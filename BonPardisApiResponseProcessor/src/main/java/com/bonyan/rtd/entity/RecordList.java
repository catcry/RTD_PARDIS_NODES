package com.bonyan.rtd.entity;

import java.util.ArrayList;
import java.util.Map;

public class RecordList extends ArrayList<Map.Entry<String, Integer>> {


    public String getkeySetString() {
        StringBuilder keySetString = new StringBuilder("[");
        for (Map.Entry<String, Integer> entry : this) {
            keySetString.append("{\"mobileNo\": \"").append(entry.getKey()).append("\"},");
        }
        keySetString.deleteCharAt(keySetString.lastIndexOf(","));
        keySetString.append("]");
        return keySetString.toString();
    }

    public String getKeyValueListString() {
        StringBuilder keySetString = new StringBuilder("{");
        for (Map.Entry<String, Integer> entry : this) {
            keySetString.append("(")
                    .append(entry.getKey()).append(",")
                    .append(entry.getValue()).append(")|");
        }
        keySetString.deleteCharAt(keySetString.lastIndexOf("|"));
        keySetString.append("}");
        return keySetString.toString();
    }
}
