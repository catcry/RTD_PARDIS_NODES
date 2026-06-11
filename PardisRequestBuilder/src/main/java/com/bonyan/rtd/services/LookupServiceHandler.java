package com.bonyan.rtd.services;

import com.bonyan.rtd.PardisRequestBuilder;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.*;
import com.comptel.mc.node.lookup.*;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;

import java.util.List;
import java.util.logging.Logger;



import java.util.List;

public class LookupServiceHandler {

    private static final Logger logger = Logger.getLogger(PardisRequestBuilder.class.getName());
    private static final TxeLogger nodeLogger = NodeLoggerFactory.getNodeLogger(PardisRequestBuilder.class.getCanonicalName());
    private final LookupTable contentTable;

    public LookupServiceHandler (LookupTable contentTable){
        this.contentTable = contentTable;

    }


    public String doLookup(String action_id, String channel, String smsLanguage) throws Exception{
        String messageContent="";
        try {
            List<NormalLookupResultItem> result = contentTable.lookup(3, 2, 1, action_id, channel);
            // int1 : return type columns returned per row
            // int2 : key type columns returned per row
            // int3 : max matching rows that should be returned
            if (result == null || result.isEmpty()) {
                throw new RuntimeException("Record not found in the lookup table: " + contentTable.getName());
            }

            if (smsLanguage.equals("English")) {
                messageContent = result.get(0).getReturnValues().get(0);
            }
            else {
                messageContent = result.get(0).getReturnValues().get(1);
            }


        } catch (RuntimeException e) {
            messageContent = "";
        }
        return messageContent;
    }
}
