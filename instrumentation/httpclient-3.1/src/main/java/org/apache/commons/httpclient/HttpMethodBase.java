/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.commons.httpclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.agent.bridge.external.URISupport;
import com.newrelic.agent.tracers.IgnoreChildSocketCalls;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient31.InboundWrapper;
import com.nr.agent.instrumentation.httpclient31.OutboundWrapper;

import java.io.IOException;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass)
public abstract class HttpMethodBase implements HttpMethod {

    @NewField
    private static final String LIBRARY = "CommonsHttp";

    // these are required to use in the module test
    public abstract void addRequestHeader(String headerName, String headerValue);
    public abstract void setRequestHeader(String headerName, String headerValue);

    @Trace(leaf = true)
    public int execute(HttpState state, HttpConnection conn) throws IOException {
        String host = null;
        String uri = null;

        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        Transaction tx = AgentBridge.getAgent().getTransaction();
        if (!checkForIgnoredSocketCall(method)) {
            // URI calculation logic migrated from old pointcut to maintain parity
            URI methodURI = getURI();
            String scheme = methodURI.getScheme();
            if (scheme == null) {
                scheme = conn.getProtocol().getScheme();
                host = conn.getHost();
                String path = methodURI.getPath();
                if ("null".equals(path)) {
                    path = null;
                }
                uri = URISupport.getURI(scheme, host, conn.getPort(), path);
            } else {
                host = methodURI.getHost();
                uri = URISupport.getURI(methodURI.getScheme(), host, conn.getPort(), methodURI.getPath());
            }

            // Set cross process headers for this outbound request
            method.addOutboundRequestHeaders(new OutboundWrapper(this));
        }

        int responseCode = Weaver.callOriginal();

        if (!checkForIgnoredSocketCall(method) && uri != null) {
            try {
                InboundWrapper inboundHeaders = new InboundWrapper(this);
                java.net.URI netURI = java.net.URI.create(uri);
                method.reportAsExternal(HttpParameters
                        .library(LIBRARY)
                        .uri(netURI)
                        .procedure("execute")
                        .inboundHeaders(inboundHeaders)
                        .build());
            } catch (Throwable e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, e,
                        "Unable to reportAsExternal for execute()");
            }
        }

        return responseCode;
    }

    @Trace(leaf = true)
    public byte[] getResponseBody() throws IOException {
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        if (!checkForIgnoredSocketCall(method)) {
            try {
                String host = getHostname();
                URI methodURI = getURI();
                String uri = URISupport.getURI(methodURI.getScheme(), methodURI.getHost(), methodURI.getPort(),
                        methodURI.getPath());

                // This method doesn't have any network I/O so we are explicitly not recording external rollup metrics
                ExternalMetrics.makeExternalComponentMetric(method, host, LIBRARY, false, uri, "getResponseBody");
            } catch (Throwable e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, e,
                        "Unable to record external metrics for getResponseBody()");
            }
        }

        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public byte[] getResponseBody(int maxlen) throws IOException {
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        if (!checkForIgnoredSocketCall(method)) {
            try {
                String host = getHostname();
                URI methodURI = getURI();
                String uri = URISupport.getURI(methodURI.getScheme(), methodURI.getHost(), methodURI.getPort(),
                        methodURI.getPath());

                // This method doesn't have any network I/O so we are explicitly not recording external rollup metrics
                ExternalMetrics.makeExternalComponentMetric(method, host, LIBRARY, false, uri, "getResponseBody");
            } catch (Throwable e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, e,
                        "Unable to record external metrics for getResponseBody()");
            }
        }

        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void releaseConnection() {
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        if (!checkForIgnoredSocketCall(method)) {
            try {
                String host = getHostname();
                URI methodURI = getURI();
                String uri = URISupport.getURI(methodURI.getScheme(), methodURI.getHost(), methodURI.getPort(),
                        methodURI.getPath());

                // This method doesn't have any network I/O so we are explicitly not recording external rollup metrics
                ExternalMetrics.makeExternalComponentMetric(method, host, LIBRARY, false, uri, "releaseConnection");
            } catch (Throwable e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, e,
                        "Unable to record external metrics for releaseConnection()");
            }
        }

        Weaver.callOriginal();
    }

    // Logic migrated from old pointcut to maintain parity
    private String getHostname() {
        Header hostHeader = getRequestHeader("host");
        if (hostHeader != null) {
            String host = hostHeader.getValue();
            // JAVA-106: if the host is an ip address, it has :<port> at the end
            // For example:
            // 172.16.2.132:8080
            // news.yahoo.com
            int index = host.indexOf(":");
            if (index > -1) {
                host = host.substring(0, index);
            }
            return host;
        }

        try {
            return getURI().getHost();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // This is here as somewhat of a workaround for now until we've migrated over all
    // pointcuts that use the IgnoreChildSocketCalls interface. At that point we should
    // re-evaluate a better & cleaner weaved module approach.
    private boolean checkForIgnoredSocketCall(TracedMethod tracedMethod) {
        if (tracedMethod != null) {
            TracedMethod parentTracedMethod = tracedMethod.getParentTracedMethod();
            return parentTracedMethod != null && parentTracedMethod instanceof IgnoreChildSocketCalls;
        }
        return false;
    }
}
