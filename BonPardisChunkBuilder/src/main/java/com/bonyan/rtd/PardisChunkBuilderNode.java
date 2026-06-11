package com.bonyan.rtd;

import com.bonyan.rtd.service.ChunkerService;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.*;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;

import java.util.logging.Logger;

public class PardisChunkBuilderNode extends Nodebase implements BusinessLogic, TimerObserver {

    private static final TxeLogger nodeLogger = NodeLoggerFactory.getNodeLogger(PardisChunkBuilderNode.class.getCanonicalName());
    private NodeContext nodeContext;
    private ChunkerService chunkerService;
    private EventRecordService eventRecordService;
    private PardisChunkBuilderNode.NodeParameters nodeParams;

    @Override
    public void init(NodeContext nodeContext) {
        try {
            nodeLogger.info("nodeLogger: node init start");

            this.nodeContext = nodeContext;
            loadNodeParameters();
            this.chunkerService = new ChunkerService(eventRecordService, nodeParams);

            nodeLogger.info("nodeLogger: node init end");
        } catch (Exception exception) {
            this.nodeContext.writeMessage("InitException: " + exception.getMessage());
            nodeLogger.info("nodeLogger: Exception in Init method: " + exception.getMessage());
        }
    }

    private void loadNodeParameters() {
        this.nodeParams = new NodeParameters();
        nodeLogger.info("nodeLogger: nodeContext.getParameters:" + nodeContext.getParameters());
    }

    @Override
    public void process(EventRecord eventRecord) {
        chunkerService.addRecord(eventRecord);
    }

    @Override
    public void flush() {
        nodeLogger.info("nodeLogger: node flush start");
    }

    @Override
    public void end() {
        nodeLogger.info("nodeLogger: node 'end' start");

    }

    @Override
    public void request(String s) {
        nodeLogger.info("nodeLogger: node request start, input String: " + s);

    }

    @Override
    public void pause(int i) {
        nodeLogger.info("nodeLogger: pause method, input int: " + i);

    }

    @Override
    public void resume(int i) {
        nodeLogger.info("nodeLogger: resume method, input int: " + i);

    }

    @Override
    public void setService(EventRecordService eventRecordService) {
        nodeLogger.info("nodeLogger: node setService method start");
        this.eventRecordService = eventRecordService;
    }

    @Override
    public void timer() {
        chunkerService.writeChunkRecords();
        o_end();
    }

    public class NodeParameters {
        private final Integer maxChunkSizeParam;
        private final Integer maxSendRetryCount;
        private final Integer maxUntouchedRecordCount;

        public NodeParameters() {
            this.maxChunkSizeParam =  nodeContext.getParameterInt("chunker-max-size");
            this.maxSendRetryCount = nodeContext.getParameterInt("max-retry-count");
            this.maxUntouchedRecordCount = nodeContext.getParameterInt("max-untouched-record-count");
        }

        public Integer getMaxChunkSizeParam() {
            return maxChunkSizeParam;
        }

        public Integer getMaxSendRetryCount() {
            return maxSendRetryCount;
        }

        public Integer getMaxUntouchedRecordCount() {
            return maxUntouchedRecordCount;
        }
    }

}
