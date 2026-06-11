package com.bonyan.rtd;

import com.bonyan.rtd.service.ResponseProcessorService;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.*;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;

public class PardisResponseProcessorNode extends Nodebase implements BusinessLogic, Schedulable {

    private static final TxeLogger nodeLogger = NodeLoggerFactory.getNodeLogger(PardisResponseProcessorNode.class.getCanonicalName());

    private NodeContext nodeContext;
    private EventRecordService eventRecordService;
    private ResponseProcessorService responseProcessorService;
    private PardisResponseProcessorNode.NodeParameters nodeParameters;
    
    @Override
    public void init(NodeContext nodeContext){
        nodeLogger.info("nodeLogger: node init start");
        this.nodeContext=nodeContext;
        loadNodeParameters();
        this.responseProcessorService = new ResponseProcessorService(eventRecordService, nodeContext, nodeParameters);
    }
    private void loadNodeParameters() {
        this.nodeParameters = new NodeParameters();
        nodeLogger.info("nodeLogger: nodeContext.getParameters:" + nodeContext.getParameters());
    }
    @Override
    public void flush(){
        nodeLogger.info("nodeLogger: node flush start");
    }

    @Override
    public void end(){
        nodeLogger.info("nodeLogger: node 'end' start");
    }

    @Override
    public void request(String s){
        nodeLogger.info("nodeLogger: node request start, input String: " + s);
    }

    @Override
    public void pause(int i){
        nodeLogger.info("nodeLogger: pause method, input int: " + i);
    }

    @Override
    public void resume(int i){
        nodeLogger.info("nodeLogger: resume method, input int: " + i);
    }

    @Override
    public void process(EventRecord eventRecord){

        responseProcessorService.processResponseRecord();
    }

    @Override
    public void setService(EventRecordService eventRecordService){
        nodeLogger.info("nodeLogger: node setService method start");
        this.eventRecordService = eventRecordService;
    }

    @Override
    public void schedule(){
        nodeLogger.info("nodeLogger: node schedule start");
    }


    public class NodeParameters {
        private final Integer maxSendRetryCount;
        public NodeParameters() {
            this.maxSendRetryCount = nodeContext.getParameterInt("max-retry-count");
        }
        public Integer getMaxSendRetryCount() {
            return maxSendRetryCount;
        }
    }
}