/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.HeadersUtil;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;

class HttpTestServerImpl extends NanoHTTPD implements HttpTestServer {
    private final int port;

    public HttpTestServerImpl() throws IOException {
        this(getRandomPort());
    }

    private static int getRandomPort() {
        int port;

        try {
            ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to allocate ephemeral port");
        }
        return port;
    }

    public HttpTestServerImpl(int port) throws IOException {
        super(port);
        this.port = port;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, ?> incomingParameters = session.getParameters();
        if (incomingParameters.containsKey(NO_TRANSACTION)) {
            return serveNonDispatcher(session);
        } else {
            return serveDispatcher(session);
        }
    }

    /**
     * This adds another transaction for every request that is made to the server.
     */
    @Trace(dispatcher = true)
    private Response serveDispatcher(IHTTPSession session) {
        return serveInternal(session);
    }

    private Response serveNonDispatcher(IHTTPSession session) {
        return serveInternal(session);
    }

    private Response serveInternal(IHTTPSession session) {
        NewRelic.addCustomParameter("server.port", this.port);
        final Map<String, String> incomingHeaders = session.getHeaders();
        if (incomingHeaders.containsKey(SLEEP_MS_HEADER_KEY)) {
            try {
                long input = Long.parseLong(incomingHeaders.remove(SLEEP_MS_HEADER_KEY));
                Thread.sleep(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean doCat = true;
        if (incomingHeaders.containsKey(DO_CAT)) {
            try {
                doCat = Boolean.parseBoolean(incomingHeaders.remove(DO_CAT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Boolean.parseBoolean(incomingHeaders.get(DO_BETTER_CAT)) && AgentBridge.getAgent().getTransaction(false) != null) {
            String tracePayload = incomingHeaders.get(HeadersUtil.NEWRELIC_TRACE_HEADER);
            if (tracePayload != null) {
                NewRelic.getAgent().getTransaction().acceptDistributedTracePayload(tracePayload);
            }
        }

        if (doCat && AgentBridge.getAgent().getTransaction(false) != null) {
            Transaction tx = NewRelic.getAgent().getTransaction();
            tx.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "ExternalHTTPServer");
            tx.setWebRequest(new RequestWrapper(session));

            Response res = newFixedLengthResponse("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n");

            // outbound cat
            com.newrelic.agent.Transaction.getTransaction().getCrossProcessTransactionState()
                    .processOutboundResponseHeaders(new OutboundWrapper(res), -1L);
            return res;
        } else {
            return newFixedLengthResponse("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n");
        }
    }

    @Override
    public void shutdown() {
        stop();
    }

    @Override
    public URI getEndPoint() throws URISyntaxException {
        return new URI("http://localhost:" + port + "/");
    }

    @Override
    public void close() {
        shutdown();
    }

    static class OutboundWrapper implements OutboundHeaders {

        private final Response delegate;

        public OutboundWrapper(Response request) {
            this.delegate = request;
        }

        @Override
        public void setHeader(String name, String value) {
            delegate.addHeader(name, value);
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }


    static class RequestWrapper extends ExtendedRequest {
        private IHTTPSession session;

        public RequestWrapper(IHTTPSession session) {
            super();
            this.session = session;
        }

        @Override
        public String getRequestURI() {
            return session.getUri();
        }

        @Override
        public String getHeader(String name) {
            return session.getHeaders().get(name);
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(session.getParameters().keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return session.getParameters().get(name).toArray(new String[0]);
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getMethod() {
            return session.getMethod().name();
        }
    }

    @Override
    public String getServerTransactionName() {
        return "WebTransaction/Custom/ExternalHTTPServer";
    }

    public String getCrossProcessId() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getCrossProcessConfig().getCrossProcessId();
    }

}
