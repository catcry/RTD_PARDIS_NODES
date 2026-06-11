package com.bonyan.rtd.service;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import com.bonyan.rtd.RestClient;
import com.bonyan.rtd.Utils;
import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.Field;
import com.comptel.mc.node.TransactionContext;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JettySingleRequestHandler extends AbstractHandler {
    public static final String HEADER = "Header-";
    public static final String COOKIE = "Cookie-";
    public static final String INTERFACE_OUT = "INTERFACE_OUT";
    private final RestClient nodeApplication;
    private final TxeLogger logger;
    private final EventRecordService eventRecordService;
    private final LinkedBlockingQueue<EventRecord> outputRecords;
    private final ConcurrentHashMap<String, EventRecord> inputRecords;
    private final int serverRequestSleepTime;
    private final int serverRequestTimeout;
    private final boolean shutdownStarted;

    public JettySingleRequestHandler(RestClient nodeApplication) {
        this.nodeApplication = nodeApplication;
        this.logger = NodeLoggerFactory.getNodeLogger(JettySingleRequestHandler.class.getCanonicalName());
        this.eventRecordService = nodeApplication.getEventRecordService();
        this.outputRecords = nodeApplication.getOutputRecords();
        this.inputRecords = (ConcurrentHashMap<String, EventRecord>) nodeApplication.getInputRecords();
        this.serverRequestSleepTime = nodeApplication.getServerRequestSleepTime();
        this.serverRequestTimeout = nodeApplication.getServerRequestTimeout();
        this.shutdownStarted = false;
    }

    public boolean isShutdownStarted() {
        return shutdownStarted;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.debug("Thread started!");

        if (shutdownStarted) {
            handleShutdown(response, baseRequest);
            return;
        }

        String requestId = Utils.getRequestId(baseRequest.hashCode());
        logger.debug("handle: requestId = {}", requestId);

        EventRecord inputER = convertRequestToEventRecord(request, response, baseRequest, requestId);
        if (inputER == null) return;  // Early exit if conversion failed


        handleRequestProcessing(request, requestId, inputER, response);

        baseRequest.setHandled(true);
        logger.debug("Thread stopped!");
    }

    private void handleShutdown(HttpServletResponse response, Request baseRequest) {
        logger.debug("handle: Shutdown Initiated, not accepting new requests...");
        response.setStatus(503);
        baseRequest.setHandled(true);
    }

    private EventRecord convertRequestToEventRecord(HttpServletRequest request, HttpServletResponse response, Request baseRequest, String requestId) {
        try {
            return convertMessageToER(request, requestId);
        } catch (IOException e) {
            logger.debug("handle: Unable to convert request to ER...");
            response.setStatus(503);
            baseRequest.setHandled(true);
            return null;
        }
    }

    private TransactionContext initializeTransaction(EventRecord inputER, HttpServletRequest request, String requestId) {
        TransactionContext transactionContext = nodeApplication.openTransaction(request, requestId);
        if (transactionContext != null) {
            nodeApplication.associateTransaction(transactionContext, requestId);
        }
        writeOutputRecord(inputER);
        logger.info("handle: Request handed over to nodebase queue.");
        nodeApplication.getIncomingRequests().add(requestId);
        logger.debug("handle: Request-Id is added to the incoming requests list.");
        return transactionContext;
    }

    private void handleRequestProcessing(HttpServletRequest request, String requestId, EventRecord inputER, HttpServletResponse response
    ) throws IOException {
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<EventRecord> answer = es.submit(new ResponseWaitTask(requestId));

        try {
            processAnswer(request, inputER, requestId, answer.get(serverRequestTimeout, TimeUnit.MILLISECONDS), response);
        } catch (InterruptedException e) {
            handleInterruptedException(e, answer);
        } catch (ExecutionException e) {
            handleExecutionException(e, answer);
        } catch (TimeoutException e) {
            handleTimeoutException(requestId, inputER, response, answer);
        } finally {
            es.shutdown();  // Ensure the executor is properly shut down
        }
    }

    private void processAnswer(HttpServletRequest request, EventRecord inputER, String requestId,
                               EventRecord answerER, HttpServletResponse response) {
        logger.debug("handle: Answer received from nodebase queue.");
        nodeApplication.getIncomingRequests().remove(requestId);
        logger.debug("handle: Request-Id is removed from the incoming requests list.");
        final TransactionContext transactionContext = initializeTransaction(inputER, request, requestId);


        if (transactionContext == null) {
            convertERToMessage(answerER, response);
        } else {
            synchronized (transactionContext) {
                if (!transactionContext.isClosed()) {
                    convertERToMessage(answerER, response);
                    nodeApplication.closeTransaction(transactionContext);
                } else {
                    logger.warn("Transaction was already closed.");
                }
            }
        }
    }

    private void handleInterruptedException(InterruptedException e, Future<EventRecord> answer) {
        logger.warn("InterruptedException: {}", e.getLocalizedMessage());
        Logger.getLogger(JettySingleRequestHandler.class.getName()).log(Level.SEVERE, null, e);
        answer.cancel(true);
        Thread.currentThread().interrupt();  // Restore interrupted status
    }

    private void handleExecutionException(ExecutionException e, Future<EventRecord> answer) {
        logger.warn("ExecutionException: {}", e.getLocalizedMessage());
        Logger.getLogger(JettySingleRequestHandler.class.getName()).log(Level.SEVERE, null, e);
        answer.cancel(true);
    }

    private void handleTimeoutException(String requestId, EventRecord inputER, HttpServletResponse response, Future<EventRecord> answer) {
        if (outputRecords != null && outputRecords.remove(inputER)) {
            logger.info("handle: Request removed from nodebase queue.");
        }
        response.setStatus(504);
        nodeApplication.getNodeContext().writeMessage("RESTIF102", requestId);
        logger.warn("handle: Server timed out while waiting for answer to requestId = {}", requestId);
        nodeApplication.getIncomingRequests().remove(requestId);
        logger.debug("handle: Request-Id is removed from the incoming requests list.");
        answer.cancel(true);
    }

    private EventRecord convertMessageToER(HttpServletRequest request, String requestId) throws IOException {
        logger.debug("convertMessageToER: Converting message to ER...");

        EventRecord eventRecord = createBaseEventRecord(request, requestId);
        addRequestHeadersToEventRecord(request, eventRecord);
        addRequestParametersToEventRecord(request, eventRecord);
        addCookiesToEventRecord(request, eventRecord);
        addRequestBodyToEventRecord(request, eventRecord);

        logger.debug("convertMessageToER: Message converted...");
        return eventRecord;
    }

    private EventRecord createBaseEventRecord(HttpServletRequest request, String requestId) {
        EventRecord eventRecord = eventRecordService.newRecord();
        eventRecord.setInputType("REST_IFACE");
        eventRecord.setOutputType("REST_IFACE");

        addFieldWithLogging(eventRecord, "Request", "true");
        addFieldWithLogging(eventRecord, "Request-Id", requestId);
        addFieldWithLogging(eventRecord, "Method", request.getMethod());
        addFieldWithLogging(eventRecord, "Remote-Scheme", request.getScheme());
        addFieldWithLogging(eventRecord, "Remote-Address", request.getRemoteAddr());
        addFieldWithLogging(eventRecord, "Remote-Port", Integer.toString(request.getRemotePort()));

        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            addFieldWithLogging(eventRecord, "Path-Info", pathInfo);
        }

        String queryString = request.getQueryString();
        if (queryString != null) {
            addFieldWithLogging(eventRecord, "Query-String", queryString);
        }

        addFieldWithLogging(eventRecord, "Request-Uri", request.getRequestURI());
        addFieldWithLogging(eventRecord, "Local-Address", request.getLocalAddr());
        addFieldWithLogging(eventRecord, "Local-Port", Integer.toString(request.getLocalPort()));

        return eventRecord;
    }

    private void addRequestHeadersToEventRecord(HttpServletRequest request, EventRecord eventRecord) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            addFieldWithLogging(eventRecord, HEADER + headerName, headerValue);
        }
    }

    private void addRequestParametersToEventRecord(HttpServletRequest request, EventRecord eventRecord) {
        Map<String, String[]> requestParameters = request.getParameterMap();
        if (requestParameters != null) {
            for (Map.Entry<String, String[]> entry : requestParameters.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    addFieldWithLogging(eventRecord, "Parameter-" + key, value);
                }
            }
        }
    }

    private void addCookiesToEventRecord(HttpServletRequest request, EventRecord eventRecord) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                addFieldWithLogging(eventRecord, COOKIE + cookie.getName(), cookie.getValue());
            }
        }
    }

    private void addRequestBodyToEventRecord(HttpServletRequest request, EventRecord eventRecord) throws IOException {
        try {
            String requestBody = getRequestBody(request);
            addFieldWithLogging(eventRecord, "Body", requestBody);
        } catch (IOException e) {
            logger.error("Error reading request body", e);
            throw e;
        }
    }

    private void addFieldWithLogging(EventRecord eventRecord, String key, String value) {
        eventRecord.addField(key, value);
        logger.debug("{} {}", key, value);
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        this.logger.debug("getRequestBody: Reading request body...");
        StringBuilder requestBody = new StringBuilder();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                requestBody.append(line);
            }

            return requestBody.toString();
        } catch (IOException exception) {
            this.nodeApplication.getNodeContext().writeMessage("RESTIF110", exception.getLocalizedMessage());
            this.logger.warn("getRequestBody: Unable to extract request body.");
            throw new IOException(exception);
        }
    }

    private void writeOutputRecord(EventRecord inputER) {
        if (this.outputRecords == null) {
            this.logger.info("writeOutputRecord: Writing output record to link: " + INTERFACE_OUT);
            if (this.nodeApplication != null) {
                this.nodeApplication.txRebindAndInherit(inputER);
            }

            this.eventRecordService.write(INTERFACE_OUT, inputER);
            this.logger.info("writeOutputRecord: TXE CTX check: %s", inputER.getTransactionContext().getId());
        } else {
            this.outputRecords.add(inputER);
            this.logger.info("writeOutputRecord: Record has been added to output record queue for link: " + INTERFACE_OUT);
        }

    }

    private void convertERToMessage(EventRecord answerER, HttpServletResponse response) {
        String status = Utils.getErField(answerER, "Status");
        this.logger.info("convertERToMessage: Response Status: " + status);
        if (status.isEmpty()) {
            response.setStatus(0);
        } else {
            response.setStatus(Integer.parseInt(status));
        }

        Field contentTypeField = answerER.getField("Header-Content-Type");
        if (contentTypeField != null) {
            String contentType = contentTypeField.getValue();
            if (!contentType.isEmpty()) {
                this.logger.info("convertERToMessage: Response Content Type: " + contentType);
                response.setContentType(contentType);
            }
        }

        Iterator<Field> iterator = answerER.getFields().iterator();

        String errMsg;
        Field responseBodyField;
        while (iterator.hasNext()) {
            responseBodyField = iterator.next();
            String name = responseBodyField.getName();
            String value = responseBodyField.getValue();
            this.logger.debug(name + " " + value);
            if (name.startsWith(HEADER)) {
                errMsg = name.substring(7);
                response.addHeader(errMsg, value);
            }

            if (name.startsWith(COOKIE)) {
                errMsg = name.substring(7);
                response.addCookie(new Cookie(errMsg, value));
            }
        }

        responseBodyField = answerER.getField("Body");
        if (responseBodyField != null) {
            String responseBody = responseBodyField.getValue();
            this.logger.info("Response Body: " + responseBody);

            try {
                PrintWriter writer = response.getWriter();
                writer.print(responseBody);
            } catch (IOException exception) {
                errMsg = exception.getLocalizedMessage();
                this.nodeApplication.getNodeContext().writeMessage("RESTIF111", errMsg);
                this.sendERErrorMessage(answerER, errMsg);
                this.logger.warn("convertERToMessage: Unable to get response body writer." + errMsg);
            }
        }

    }

    public void sendERErrorMessage(EventRecord inputER, String errMsg) {
        this.logger.info("Write to ER_OUTPUT_LINK link Error er");
        EventRecord errorER = (EventRecord) inputER.copy();
        errorER.setInputType("REST_IFACE_ERROR");
        errorER.setOutputType("REST_IFACE_ERROR");
        errorER.addField("Error-Status", Integer.toString(501));
        errorER.addField("Error-Description", errMsg);
        if (this.nodeApplication != null) {
            this.nodeApplication.txRebindAndInherit(errorER);
        }

        this.eventRecordService.write(INTERFACE_OUT, errorER);
        this.logger.info("done Write to ER_OUTPUT_LINK link Error");
    }

    private class ResponseWaitTask implements Callable<EventRecord> {
        String requestId;

        public ResponseWaitTask(String requestId) {
            this.requestId = requestId;
        }

        public EventRecord call() throws Exception {
            EventRecord answerER = null;

            do {
                JettySingleRequestHandler.this.logger.debug("ResponseWaitTask::call: About to poll for answer...");
                if (JettySingleRequestHandler.this.inputRecords != null) {
                    JettySingleRequestHandler.this.logger.debug("ResponseWaitTask:call: Retrieving answer for '" + this.requestId + "'...");
                    answerER = JettySingleRequestHandler.this.inputRecords.remove(this.requestId);
                }

                if (answerER == null) {
                    JettySingleRequestHandler.this.logger.debug("ResponseWaitTask:call: Sleeping for " + JettySingleRequestHandler.this.serverRequestSleepTime + " milliseconds...");
                    Thread.sleep(JettySingleRequestHandler.this.serverRequestSleepTime);
                }
            } while (answerER == null);

            return answerER;
        }
    }
}
