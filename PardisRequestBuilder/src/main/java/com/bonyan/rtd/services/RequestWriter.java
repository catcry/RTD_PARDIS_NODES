package com.bonyan.rtd.services;

import com.bonyan.rtd.PardisRequestBuilder;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;

public class RequestWriter extends Nodebase {

    private static final TxeLogger nodeLogger = NodeLoggerFactory.getNodeLogger(PardisRequestBuilder.class.getCanonicalName());
    public EventRecordService erService;
    public EventRecord inputRecord;
    public String requestBody;
    public  EventRecord outRecord;
    public String endpointURI;
    public RequestWriter(EventRecordService erService,EventRecord inputEventRecord,String requestBody, String endpointURI) {
        this.erService = erService;
        this.inputRecord = inputEventRecord;
        this.requestBody = requestBody;
        this.outRecord = (EventRecord) this.inputRecord.copy();
        this.endpointURI = endpointURI;

    }

    public void buildOutRecord () {
        this.outRecord.addField("Method", "POST");
        this.outRecord.addField("Request", "True");
        this.outRecord.addField("Body",this.requestBody);
        this.outRecord.addField("MMMHeader-User-Agent", "curl/7.19.7 (x86_64-redhat-linux-gnu)libcurl/7.19.7 NSS/3.14.0.0 zlib/1.2.3 libidn/1.18libssh2/1.4.2");
        this.outRecord.addField("Header-Content-Type", "application/json; charset=UTF-8");
        this.outRecord.addField("Header-Accept", "application/json");
        this.outRecord.addField("Request-Uri", this.endpointURI);
    }

    public void writeOutRecord(){
        try{
            buildOutRecord();
            erService.write("OUT", this.outRecord);

            //erService.write(outputRecord);
        }
        catch (RuntimeException e) {
            nodeLogger.error("Error in building/writing output record: ");
            this.inputRecord.reject("WRITE_ERROR","Building/Writing record failed. Error: "+ e.getMessage());
        }
    }

}
