package com.bonyan.rtd.services;


import com.bonyan.rtd.dto.BodyDto;
import java.util.List;
import java.util.logging.Logger;

/*
import com.bonyan.rtd.PardisRequestBuilder;
import com.bonyan.rtd.services.LookupServiceHandler;
import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;
import com.comptel.mc.node.lookup.LookupService;
import com.comptel.mc.node.lookup.LookupTable;
*/

/*
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
*/

public class RequestBodyBuilder {

    /*
    private static final Logger logger = Logger.getLogger(PardisRequestBuilder.class.getName());
    private static final TxeLogger nodeLogger = NodeLoggerFactory.getNodeLogger(PardisRequestBuilder.class.getCanonicalName());
    public EventRecordService erService;
    private String lookupServerName;
    private String lookupTableName;
    private LookupService lookupService;
    private LookupTable contentTable;
    private EventRecord eventRecord;
    private LookupServiceHandler lookupServiceHandler;
    public String channel;
    */


    public RequestBodyBuilder () {

    }
    public String buildBody(BodyDto bodyDto) {

        // ------    jackson implementation   ----

        /*
        ObjectMapper objectMapper = new ObjectMapper();
        try {
             Convert the SendSmsRequest object to a JSON string
            return objectMapper.writeValueAsString(bodyDto);

        } catch (JsonProcessingException e) {
            nodeLogger.error("Error in building body ");
            return null;
        }
        String check;
        check = "HelloCheck";
        return check;
        */

        // ------  plain String implementation   ------
        StringBuilder json = new StringBuilder();
        json.append("{");
        // Adding destinationList
        json.append("\"destinationList\": [");
        List<String> destinationList = bodyDto.getDestinationList();
        for (int i = 0; i < destinationList.size(); i++) {
            json.append("{\"mobileNo\": \"").append(destinationList.get(i)).append("\"}");
            if (i < destinationList.size() - 1) {
                json.append(", ");
            }
        }
        json.append("], ");

        // Adding message
        json.append("\"message\": \"").append(bodyDto.getMessage()).append("\", ");

        // Adding smsClass
        json.append("\"smsClass\": \"").append(bodyDto.getSmsClass()).append("\", ");

        // Adding source
        json.append("\"source\": \"").append(bodyDto.getSource()).append("\"");

        json.append("}");

        return json.toString();
        }
    }




