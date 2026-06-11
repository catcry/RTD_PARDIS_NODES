package com.bonyan.rtd.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonParser {

    private JsonParser() {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> getJsonValues(String json) {
        List<String> values = new ArrayList<>();
        json = json.trim().replaceAll("[\\[\\]{\"}\\s]", "");
        json = json.trim().replace("smsIdList:", "");

        if(json.contains(",")) {
            values = Arrays.asList(json.split(","));
        } else {
            values.add(json);
        }
        return values;
    }
}
