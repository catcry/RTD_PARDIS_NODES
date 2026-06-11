package com.bonyan.rtd.dto;

import java.util.List;
import java.util.ArrayList;
public class BodyDto {

    private List<String> destinationList;
    private String message;
    private String smsClass;
    private String source;

    public String getMessage() {
        return message;
    }

    public List<String> getDestinationList() {
        return destinationList;
    }

    public void setDestinationList(List<String> destinationList) {
        this.destinationList = destinationList;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSmsClass() {
        return smsClass;
    }

    public void setSmsClass(String smsClass) {
        this.smsClass = smsClass;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
