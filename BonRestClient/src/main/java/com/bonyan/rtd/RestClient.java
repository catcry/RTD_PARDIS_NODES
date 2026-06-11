package com.bonyan.rtd;

import com.bonyan.rtd.service.JettyClient;
import com.bonyan.rtd.service.JettySingleRequestHandler;
import com.bonyan.rtd.token.ApiInfo;
import com.bonyan.rtd.token.Token;
import com.bonyan.rtd.token.TokenDurationType;
import com.bonyan.rtd.token.TokenType;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.*;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class RestClient extends Nodebase implements BusinessLogic, Schedulable
        , TransactionEndpoint, TimerObserver {
    public static final String OUTPUT_LINK = "INTERFACE_OUT";
    public static final String REJECTED_STORAGE = "REJECTED";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final TxeLogger logger = NodeLoggerFactory.getNodeLogger(RestClient.class.getCanonicalName());
    private final Object transactionLock = new Object();
    long lastCheck = System.currentTimeMillis();
    int counter = 0;
    EventRecord lastEventRecord = null;
    private int clientRequestTimeout;
    private int clientResponseContentBufferSize;
    private int serverRequestSleepTime;
    private int serverRequestTimeout;
    private NodeContext nodeContext;
    private EventRecordService eventRecordService;
    private LinkedBlockingQueue<EventRecord> outputRecords;
    private List<String> incomingRequests;
    private ConcurrentHashMap<String, EventRecord> inputRecords;
    private int maxNumberOfServerThreads;
    private String jettyDiagnosticLevel;
    private boolean useJettyStdErrLog;
    private TransactionManager transactionManager;
    private Server jettyServer;
    private JettySingleRequestHandler jettyRequestHandler;
    private ContextHandlerCollection contexts;
    private JettyClient client;
    private Map<TransactionContext, OpenTransaction> openTransactions = new ConcurrentHashMap<>();
    private Map<TransactionContext, EventRecord> openNetworkTransactions = new ConcurrentHashMap<>();
    private long transactionTimeout;
    private long tokenExpireTime;
    private int sendRequestWaitTime;
    private TokenType tokenType;
    private TokenDurationType tokenDurationType;
    private int expirationDuration;
    private int renewalMarginPercentage;

    private boolean useToken;
    private ApiInfo tokenApiInfo;
    private Token token;

    @Override
    public void init(NodeContext nodeContext) throws Exception {
        logger.info("Node init");

        this.nodeContext = nodeContext;
        this.outputRecords = new LinkedBlockingQueue<>();
        this.inputRecords = new ConcurrentHashMap<>();
        this.incomingRequests = Collections.synchronizedList(new ArrayList<String>());

        this.readNodeParameters();
        this.client = new JettyClient(this);
        if (this.useJettyStdErrLog) {
            System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StrErrLog");
        }
        logger.debug("init: org.eclipse.jetty.util.log.class = " + System.getProperty("org.eclipse.jetty.util.log.class"));
        System.setProperty("org.eclipse.jetty.LEVEL", this.jettyDiagnosticLevel);
        logger.debug("init: org.eclipse.jetty.LEVEL = " + System.getProperty("org.eclipse.jetty.LEVEL"));

    }

    @Override
    public void process(EventRecord eventRecord) throws Exception {
        logger.info("process(): entered the function...");
        Field requestField = eventRecord.getField("Request");
        if (requestField == null) {
            this.processErrorRecord(eventRecord, "Request field missing from record.");
        } else {
            String request = requestField.getValue();
            if (request != null && !request.isEmpty()) {
                if (request.equalsIgnoreCase("true")) {
                    Thread.sleep(this.sendRequestWaitTime);
                    this.processRequestRecord(eventRecord);
                } else if (request.equalsIgnoreCase("false")) {
                    this.processAnswerRecord(eventRecord);
                } else {
                    this.processErrorRecord(eventRecord, "Request field has invalid value: " + request);
                }
            } else {
                this.processErrorRecord(eventRecord, "Request field is empty.");
            }
        }

    }

    private void processAnswerRecord(EventRecord er) {
        logger.info("Processing answer record:");
        logger.debug(er.toString());
        String status = Utils.getErField(er, "Status");
        if (status.isEmpty()) {
            this.processErrorRecord(er, "Status field is missing or empty.");
        } else {
            String requestId = Utils.getErField(er, "Request-Id");
            if (requestId.isEmpty()) {
                this.processErrorRecord(er, "Request-Id field is missing or empty.");
            } else if (this.inputRecords == null) {
                this.nodeContext.writeMessage("RESTIF015");
                logger.error("Map of input records isn't allocated.");
                throw new InvalidParameterException("Map of input records isn't allocated.");
            } else if (!this.incomingRequests.contains(requestId)) {
                this.processErrorRecord(er, "Input record mapping for this Request-Id is not present.");
            } else {
                this.inputRecords.put(requestId, er);
                logger.info("ER put to map of answers");
            }
        }
    }


    private void processRequestRecord(EventRecord er) {
        logger.info("Processing request record... Token 1.2.2 " + this.lastCheck);
        this.lastEventRecord = er;
        if (this.isUseToken()) {
            logger.info("Processing request record... found a Token method fix 18AUG v1");
            if (this.token == null || !token.isTokenValid()) {
                this.token = this.client.getToken(er);
                logger.info("OBTAIN_NEW_TOKEN");
                er.addField("Header-Authorization", "Bearer " + this.token.getTokenValue());
            } else {
                logger.info("USE_CURRENT_TOKEN_" + this.token.getTokenValue());
                er.addField("Header-Authorization", "Bearer " + this.token.getTokenValue());
            }
        } else {
            logger.info("Processing request record... can't find  a Token method");
        }

        this.client.createRESTClientConnection(er, this.eventRecordService);
    }

    String extractAndEncodeToken(String token) {
        logger.info("extractAndEncodeToken");

        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(token);
            JSONObject jsonObject = (JSONObject) obj;
            String encoded = "";
            String tokenId = (String) jsonObject.get("TokenID");
            logger.info("Token Value " + tokenId);
            String generatedToken = "";
            String stringText = "CAMPAIGN|Campaign123|" + tokenId;
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(stringText.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; ++i) {
                sb.append(Integer.toString((bytes[i] & 255) + 256, 16).substring(1));
            }

            generatedToken = sb.toString();
            byte[] bytes1 = ("Campaign:" + generatedToken).getBytes(StandardCharsets.UTF_8);
            encoded = Base64.encodeBase64String(bytes1);
            logger.info("Encoded Token Value " + encoded);
            return encoded;
        } catch (Exception var13) {
            Exception e = var13;
            e.printStackTrace();
            return "";
        }
    }

    private void processErrorRecord(EventRecord er, String errMsg) {
        logger.info("REJECTED: " + errMsg);
        er.reject(REJECTED_STORAGE, errMsg);
        er.setInputType("REST_IFACE_ERROR");
        er.setOutputType("REST_IFACE_ERROR");
        er.addField("Error-Status", "Not defined yet");
        er.addField("Error-Description", errMsg);
        this.eventRecordService.write(OUTPUT_LINK, er);
    }

    @Override
    public void flush() throws Exception {
        nb_trace("in node flush method", 0);
        logger.info("nodeLogger: node flush start");
    }

    @Override
    public void end() throws Exception {
        nb_trace("node end method start", 0);
        logger.info("nodeLogger: node 'end' start");
    }

    @Override
    public void request(String s) throws Exception {
        nb_trace("in node request method, input String: " + s, 0);
        logger.info("nodeLogger: node request start, input String: " + s);

    }

    @Override
    public void pause(int i) throws Exception {
        nb_trace("in node pause method, input int: " + i, 0);
        logger.info("nodeLogger: pause method, input int: " + i);

    }

    @Override
    public void resume(int i) throws Exception {
        nb_trace("in node resume method, input int: " + i, 0);
        logger.info("nodeLogger: resume method, input int: " + i);

    }

    @Override
    public void setService(EventRecordService eventRecordService) {
        nb_trace("in node setService method (EventRecordService will set here)", 0);
        logger.info("nodeLogger: node setService method start");

        this.eventRecordService = eventRecordService;
    }

    @Override
    public void schedule() throws Exception {
        nb_trace("in node schedule method", 0);
        logger.info("nodeLogger: node schedule start");

    }

    @Override
    public void timer() {
        nb_trace("no input file processing, timer method start", 0);
        logger.info("nodeLogger: no input file processing, timer method start");
    }

    public String getToken() {
        return "Bearer " + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImNhdGNyeSIsImV4cCI6MTcyODQzNzMxOH0.PZmvrcM238NtuWeKsTauA5VrJsxDGRMGnEUeQZL1YyI";
    }

    private void readNodeParameters() {
        this.useToken = this.nodeContext.getParameter("UseToken").equals("Yes");
        if (this.useToken) {
            this.setTokenProperties();
            this.setTokenApiInfo();
        }
        this.clientRequestTimeout = this.nodeContext.getParameterInt("ClientRequestTimeout");
        this.clientResponseContentBufferSize = this.nodeContext.getParameterInt("ClientResponseContentBufferSize");
        this.jettyDiagnosticLevel = this.nodeContext.getParameter("JettyDiagnosticLevel");

        if (this.nodeContext.parameterExists("ServerRequestSleepTime")) {
            this.serverRequestSleepTime = this.nodeContext.getParameterInt("ServerRequestSleepTime");
        } else {
            this.serverRequestSleepTime = 0;
        }

        if (this.nodeContext.parameterExists("ServerRequestTimeout")) {
            this.serverRequestTimeout = this.nodeContext.getParameterInt("ServerRequestTimeout");
        } else {
            this.serverRequestTimeout = 0;
        }

        if (this.nodeContext.parameterExists("SendRequestWaitTime")) {
            this.sendRequestWaitTime = this.nodeContext.getParameterInt("sendRequestWaitTime");
        } else {
            this.sendRequestWaitTime = 0;
        }

        this.useJettyStdErrLog = this.nodeContext.getParameter("UseJettyStdErrLog").equals("Yes");
    }

    public void setTokenApiInfo() {
        this.tokenApiInfo = new ApiInfo();
        HashMap<String, String> tokenUriBody = new HashMap<>();
        tokenUriBody.put("grant_type", "password");
        tokenUriBody.put("client_id", "usr-mng");
        tokenUriBody.put("username", this.nodeContext.getParameter("Token-Request-Username"));
        tokenUriBody.put("password", this.nodeContext.getParameter("Token-Request-Password"));
        this.tokenApiInfo.setMethod(this.nodeContext.getParameter("Token-Method"))
                .setRequestId(this.nodeContext.getParameter("Token-Request-Id"))
                .setRequestURI(this.nodeContext.getParameter("Token-Request-URI"))
                .setUrlEncodeBody(tokenUriBody);


    }

    public ApiInfo getTokenApiInfo() {
        return tokenApiInfo;
    }

    public void setTokenProperties() {
        if (this.nodeContext.parameterExists("TokenType")) {
            String tokenTypeName = this.nodeContext.getParameter("TokenType");
            this.tokenType = TokenType.getTokenType(tokenTypeName);
        } else {
            this.tokenType = TokenType.NORMAL_TOKEN;
        }

        if (this.nodeContext.parameterExists("ExpirationDuration")) {
            this.expirationDuration = this.nodeContext.getParameterInt("ExpirationDuration");
        } else {
            this.expirationDuration = 300;
        }

        if (this.nodeContext.parameterExists("RenewalMarginPercentage")) {
            this.renewalMarginPercentage = this.nodeContext.getParameterInt("RenewalMarginPercentage");
        } else {
            this.renewalMarginPercentage = 0;
        }

        if (this.nodeContext.parameterExists("TokenDurationType")) {
            String durationTypeName = this.nodeContext.getParameter("TokenDurationType");
            this.setTokenDurationType(durationTypeName);
        } else {
            this.tokenDurationType = TokenDurationType.SECOND;
        }
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public boolean isUseToken() {
        return useToken;
    }

    public int getRenewalMarginPercentage() {
        return renewalMarginPercentage;
    }

    public int getExpirationDuration() {
        return expirationDuration;
    }

    public TokenDurationType getTokenDurationType() {
        return tokenDurationType;
    }

    public void setTokenDurationType(String tokenDurationTypeName) {
        this.tokenDurationType = TokenDurationType.valueOf(tokenDurationTypeName);
    }

    public TransactionContext openTransaction(HttpServletRequest req, String id) {
        this.transactionTimeout = this.serverRequestTimeout;
        TransactionContext txCtx = this.transactionManager.beginTransaction(this, this.transactionTimeout);
        if (txCtx != null) {
            synchronized (txCtx) {
                if (!txCtx.isClosed()) {
                    OpenTransaction transaction = new OpenTransaction(req, id);
                    this.openTransactions.put(txCtx, transaction);
                    logger.info("New transaction %s sucessfully created for %s " +
                            "(message not associated to the transaction yet)", txCtx.getId(), id);
                }
            }
        }

        return txCtx;
    }

    public void associateTransaction(TransactionContext transactionContext, String transactionId) {
        try {
            if (transactionContext != null) {
                logger.info("Associate transaction %s to transaction identifier %s",
                        transactionContext.getId(), transactionId);
                this.transactionManager.associate(this, transactionContext, transactionId);
            }
        } catch (Exception var4) {
            Exception e = var4;
            this.transactionManager.removeAssociation(this, transactionContext, transactionId);
            logger.info("Failed to associate transaction " + transactionContext.getId() + " %s", e);
        }

    }

    public void closeTransaction(TransactionContext ctx) {
        if (ctx != null) {
            synchronized (transactionLock) {
                this.transactionManager.commitTransaction(ctx);
                this.openTransactions.remove(ctx);
                logger.info("Transaction %s closed", ctx.getId());
            }
        }
    }

    public void txRebindAndInherit(EventRecord er) {
        Field requestIdField = er.getField("Request-Id");
        if (requestIdField != null && requestIdField.getValue() != null && !requestIdField.getValue().isEmpty()) {
            try {
                this.rebindTransaction(requestIdField.getValue());
                er.setInheritTransactionEnabled(true);
            } catch (Exception exception) {
                this.nodeContext.writeMessage("RESTIF108", exception.getLocalizedMessage());
                logger.warn("EventRecord cannot be associated with Transaction, because of unknown problem: %s"
                        , er.toString());
                logger.info("Above warning is caused by java exception: ", exception);
            }
        } else {
            this.nodeContext.writeMessage("RESTIF107");
            logger.warn("EventRecord cannot be associated with Transaction " +
                    "because does not have required information: %s", er.toString());
        }

    }

    public EventRecord rebindTransaction(String transactionId) {
        TransactionContext transactionContext = this.transactionManager.rebind(this, transactionId);
        if (transactionContext != null) {
            logger.info("Rebind to transaction %s using transaction identifier %s successful",
                    transactionContext.getId(), transactionId);
            return this.openNetworkTransactions.remove(transactionContext);
        } else {
            logger.info("Rebind to transaction using trnasaction identifier %s failed", transactionId);
            return null;
        }
    }

    public void associateTransaction(EventRecord eventRecord, String transactionId) {
        TransactionContext transactionContext = eventRecord.getTransactionContext();
        if (transactionContext != null) {
            this.openNetworkTransactions.put(transactionContext, eventRecord);
        }

        this.associateTransaction(transactionContext, transactionId);
    }

    @Override
    public void prepareShutdown() throws Exception {
        logger.info("prepareShutdown started");
        logger.info("prepareShutdown end");
    }

    @Override
    public void commit(TransactionContext transactionContext) throws Exception {

        logger.info("commit started");
        logger.info("commit end");
    }

    @Override
    public void rollback(TransactionContext transactionContext, int i, String s) throws Exception {

        logger.info("rollback started");
        logger.info("rollback end");
    }

    public int getClientRequestTimeout() {
        return clientRequestTimeout;
    }

    public void setClientRequestTimeout(int clientRequestTimeout) {
        this.clientRequestTimeout = clientRequestTimeout;
    }

    public int getClientResponseContentBufferSize() {
        return clientResponseContentBufferSize;
    }

    public void setClientResponseContentBufferSize(int clientResponseContentBufferSize) {
        this.clientResponseContentBufferSize = clientResponseContentBufferSize;
    }

    public int getServerRequestSleepTime() {
        return serverRequestSleepTime;
    }

    public void setServerRequestSleepTime(int serverRequestSleepTime) {
        this.serverRequestSleepTime = serverRequestSleepTime;
    }

    public int getServerRequestTimeout() {
        return serverRequestTimeout;
    }

    public void setServerRequestTimeout(int serverRequestTimeout) {
        this.serverRequestTimeout = serverRequestTimeout;
    }

    public NodeContext getNodeContext() {
        return nodeContext;
    }

    public void setNodeContext(NodeContext nodeContext) {
        this.nodeContext = nodeContext;
    }

    public EventRecordService getEventRecordService() {
        return eventRecordService;
    }

    public void setEventRecordService(EventRecordService eventRecordService) {
        this.eventRecordService = eventRecordService;
    }

    public LinkedBlockingQueue<EventRecord> getOutputRecords() {
        return outputRecords;
    }

    public void setOutputRecords(LinkedBlockingQueue<EventRecord> outputRecords) {
        this.outputRecords = outputRecords;
    }

    public List<String> getIncomingRequests() {
        return incomingRequests;
    }

    public void setIncomingRequests(List<String> incomingRequests) {
        this.incomingRequests = incomingRequests;
    }

    public ConcurrentMap<String, EventRecord> getInputRecords() {
        return inputRecords;
    }

    public void setInputRecords(ConcurrentMap<String, EventRecord> inputRecords) {
        this.inputRecords = (ConcurrentHashMap<String, EventRecord>) inputRecords;
    }

    public int getMaxNumberOfServerThreads() {
        return maxNumberOfServerThreads;
    }

    public void setMaxNumberOfServerThreads(int maxNumberOfServerThreads) {
        this.maxNumberOfServerThreads = maxNumberOfServerThreads;
    }

    public String getJettyDiagnosticLevel() {
        return jettyDiagnosticLevel;
    }

    public void setJettyDiagnosticLevel(String jettyDiagnosticLevel) {
        this.jettyDiagnosticLevel = jettyDiagnosticLevel;
    }

    public boolean isUseJettyStdErrLog() {
        return useJettyStdErrLog;
    }

    public void setUseJettyStdErrLog(boolean useJettyStdErrLog) {
        this.useJettyStdErrLog = useJettyStdErrLog;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public void setTransactionManager(TransactionManager transactionManager) {
        logger.info("setTransactionManager started");
        logger.info("setTransactionManager end");
    }

    public Server getJettyServer() {
        return jettyServer;
    }

    public void setJettyServer(Server jettyServer) {
        this.jettyServer = jettyServer;
    }

    public JettySingleRequestHandler getJettyRequestHandler() {
        return jettyRequestHandler;
    }

    public void setJettyRequestHandler(JettySingleRequestHandler jettyRequestHandler) {
        this.jettyRequestHandler = jettyRequestHandler;
    }

    public ContextHandlerCollection getContexts() {
        return contexts;
    }

    public void setContexts(ContextHandlerCollection contexts) {
        this.contexts = contexts;
    }

    public JettyClient getClient() {
        return client;
    }

    public void setClient(JettyClient client) {
        this.client = client;
    }

    public Map<TransactionContext, OpenTransaction> getOpenTransactions() {
        return openTransactions;
    }

    public void setOpenTransactions(Map<TransactionContext, OpenTransaction> openTransactions) {
        this.openTransactions = openTransactions;
    }

    public Map<TransactionContext, EventRecord> getOpenNetworkTransactions() {
        return openNetworkTransactions;
    }

    public void setOpenNetworkTransactions(Map<TransactionContext, EventRecord> openNetworkTransactions) {
        this.openNetworkTransactions = openNetworkTransactions;
    }

    public long getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setTransactionTimeout(long transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    public long getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(long tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }

    public int getSendRequestWaitTime() {
        return sendRequestWaitTime;
    }

    public void setSendRequestWaitTime(int sendRequestWaitTime) {
        this.sendRequestWaitTime = sendRequestWaitTime;
    }

    private static class OpenTransaction {
        protected HttpServletRequest req;
        protected String id;

        OpenTransaction(HttpServletRequest req, String id) {
            this.req = req;
            this.id = id;
        }
    }
}
