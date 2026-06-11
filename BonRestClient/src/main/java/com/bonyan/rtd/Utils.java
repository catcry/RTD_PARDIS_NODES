package com.bonyan.rtd;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import com.comptel.mc.node.EventRecord;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.http.HttpFields;

public class Utils {
    public static final String BLANK = "";
    private static final AtomicLong requestCounter = new AtomicLong();

    private Utils() {

    }

    public static String getErField(EventRecord er, String currentField) {
        if (er.getField(currentField) != null) {
            return er.getField(currentField).getValue() != null ? er.getField(currentField).getValue() : "";
        } else {
            return "";
        }
    }

    public static String getHttpField(HttpFields fieList, String currentField) {
        if (fieList.getField(currentField) != null) {
            return fieList.getField(currentField).getValue() != null ? fieList.getField(currentField).getValue() : "";
        } else {
            return "";
        }
    }

    public static int getErFieldInt(EventRecord er, String currentField) throws NumberFormatException {
        if (er.getField(currentField) != null) {
            String value = er.getField(currentField).getValue();
            return value != null && !value.isEmpty() ? Integer.valueOf(er.getField(currentField).getValue()) : 0;
        } else {
            return 0;
        }
    }

    public static String getRequestId(int jobObjectHashCode) {
        return createId("" + jobObjectHashCode);
    }

    private static synchronized String createId(String addr) {
        StringBuilder sb = new StringBuilder();
        sb.append("request_");
        sb.append((new Date()).getTime() + requestCounter.getAndIncrement());
        sb.append("_");
        sb.append(addr);
        return sb.toString();
    }
}
