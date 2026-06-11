package com.bonyan.rtd;

import com.bonyan.rtd.services.RequestBodyBuilder;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.*;
import com.comptel.mc.node.lookup.*;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;


import com.bonyan.rtd.services.LookupServiceHandler;
import com.bonyan.rtd.dto.BodyDto;
import com.bonyan.rtd.services.RequestWriter;

public class PardisRequestBuilder extends Nodebase implements BusinessLogic, Schedulable, TimerObserver, LookupServiceUser {

    private static final Logger logger = Logger.getLogger(PardisRequestBuilder.class.getName());
    private static final TxeLogger nodeLogger = NodeLoggerFactory.getNodeLogger(PardisRequestBuilder.class.getCanonicalName());
    public EventRecordService erService;
    private String lookupServerName;
    private String lookupTableName;
    private LookupService lookupService;
    private LookupTable contentTable;
    private EventRecord newOutputRecord;
    private LookupServiceHandler lookupServiceHandler;
    public String channel;
    private String smsClassName;
    private String smsSource;
    public String requestBodyString;
    public String smsContent;
    public String smsLanguage;
    public String endpointURI;




    @Override
    public void setLookupService(LookupService service) {
        lookupService = service;
    }

    @Override
    public void init(NodeContext nodeContext) throws Exception {

        // -----    Node Parameters     -------------------
        lookupServerName = nodeContext.getParameter("Lookup_Server");
        lookupTableName = nodeContext.getParameter("Lookup_Table");
        smsClassName = nodeContext.getParameter("SMS_Class");
        smsSource = nodeContext.getParameter("SMS_Source_No");
        channel = nodeContext.getParameter("channel");
        smsLanguage = nodeContext.getParameter("SMS_Language");
        endpointURI = nodeContext.getParameter("API_Endpoint_URI");
        //http://192.168.5.84:4080/api/v2/sendBulk
        // -----------------------------------------------

        contentTable = lookupService.getTable(lookupServerName, lookupTableName,true);
        lookupServiceHandler = new LookupServiceHandler(contentTable);
    }

    @Override
    public void flush() throws Exception {
        nodeLogger.info("nodeLogger: node flush start");
    }

    @Override
    public void end() throws Exception {
        nodeLogger.info("nodeLogger: node 'end' start");
    }

    @Override
    public void request(String s) throws Exception {
        nodeLogger.info("nodeLogger: node request start, input String: " + s);
    }

    @Override
    public void pause(int i) throws Exception {
        nodeLogger.info("nodeLogger: pause method, input int: " + i);
    }

    @Override
    public void resume(int i) throws Exception {
        nodeLogger.info("nodeLogger: resume method, input int: " + i);

    }

    @Override
    public void process(EventRecord eventRecord) throws Exception {



        // -----   Get Content ID
        Field actionBlock = eventRecord.getField("Action");
        Field action_id_field = actionBlock.getField("content_id");
        String action_id = action_id_field.getValue();


        // ------ Process Chunk List  and create msisdn List  -----
        BodyDto bodyDto;
        bodyDto = new BodyDto();
        List<String> msisdnList = new ArrayList<>();

        Field chunkList = eventRecord.getField("ChunkList");
        //Field subTestSub1;
        //String subTest;
        //subTestSub1 = chunkList.getField("Sub1");
        //subTest = String.valueOf(subTestSub1.getField("msisdn"));
        //msisdnList.add("Amir");
        for (Field subBlock : chunkList.getFields()) {
            //String msisdn = String.valueOf(subBlock. getField("msisdn"));
            Field subName = subBlock.getField("msisdn");
            String msisdn = subName.getValue();
            msisdnList.add(msisdn);
        }
        bodyDto.setDestinationList(msisdnList);
        bodyDto.setSmsClass(smsClassName);
        bodyDto.setSource(smsSource);

        // --------------------------    SMS Content Lookup    --------------------------------------
        smsContent = lookupServiceHandler.doLookup(action_id, channel, smsLanguage);
        if (Objects.equals(smsContent, "")) {
            nodeLogger.error("Record not found in the lookup table: " + contentTable.getName());
            eventRecord.reject("LOOKUP_ERROR","Lookup table record not found.");
        }
        bodyDto.setMessage(smsContent);

        // ---------------------------        Build Body     ------------------------------------------
        RequestBodyBuilder requestBody;
        requestBody = new RequestBodyBuilder();
        requestBodyString =  requestBody.buildBody(bodyDto);


        // ------------------------     Build and Write Request Record    ------------------------------

        RequestWriter agent = new RequestWriter(erService,eventRecord,requestBodyString, endpointURI);
        agent.writeOutRecord();

        /*
        newOutputRecord= (EventRecord) eventRecord.copy();
        newOutputRecord.addField("content_id",smsContent);
        newOutputRecord.addField("Body",requestBodyString);
        erService.write("OUT",newOutputRecord);
         */

    }

    @Override
    public void setService(EventRecordService eventRecordService) {
        this.erService = eventRecordService;
    }

    @Override
    public void schedule() throws Exception {

    }

    @Override
    public void timer() throws Exception {

    }



}
