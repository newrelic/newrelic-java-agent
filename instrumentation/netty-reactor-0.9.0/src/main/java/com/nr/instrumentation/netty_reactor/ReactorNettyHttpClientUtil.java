package com.nr.instrumentation.netty_reactor;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.netty.Connection;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

/**
 * Utility methods for Reactor Netty HttpClient instrumentation.
 *
 * IMPORTANT: Uses reflection to avoid referencing HttpClientRequest/HttpClientResponse interfaces,
 * which would add io.netty types to Reference-Classes and cause the module to be skipped.
 */
public class ReactorNettyHttpClientUtil {

    private static final String LIBRARY = "ReactorNetty";
    private static final String SEGMENT_NAME = "External";

    /**
     * Start a new segment for tracking this external call.
     */
    public static Segment startSegment() {
        try {
            Transaction txn = AgentBridge.getAgent().getTransaction(false);
            return txn == null ? null : txn.startSegment(SEGMENT_NAME);
        } catch (Throwable t) {
            reportInstrumentationError(t);
            return null;
        }
    }

    /**
     * Extract URI from Connection (HttpClientOperations) using reflection.
     */
    public static URI extractURI(Connection connection) {
        try {
            Method uriMethod = connection.getClass().getMethod("uri");
            uriMethod.setAccessible(true);  // Required for Java 9+ module access
            String uriString = (String) uriMethod.invoke(connection);

            // Try resourceUrl() if uri() doesn't give us a full URI
            if (uriString == null || (!uriString.startsWith("http://") && !uriString.startsWith("https://"))) {
                try {
                    Method resourceUrlMethod = connection.getClass().getMethod("resourceUrl");
                    resourceUrlMethod.setAccessible(true);
                    String fullUrl = (String) resourceUrlMethod.invoke(connection);
                    if (fullUrl != null) {
                        return new URI(fullUrl);
                    }
                } catch (NoSuchMethodException e) {
                    // resourceUrl() not available, fall through to use relative URI
                }
            }

            return new URI(uriString);
        } catch (URISyntaxException e) {
            return null;
        } catch (Throwable t) {
            reportInstrumentationError(t);
            return null;
        }
    }

    /**
     * Add distributed tracing headers to the outbound request using reflection.
     */
    public static void addOutboundHeaders(Connection connection, Segment segment) {
        if (segment == null || connection == null) {
            return;
        }
        try {
            // Use reflection to get requestHeaders() method
            Method requestHeadersMethod = connection.getClass().getMethod("requestHeaders");
            requestHeadersMethod.setAccessible(true);
            Object headers = requestHeadersMethod.invoke(connection);

            OutboundHeadersWrapper outboundHeaders = new OutboundHeadersWrapper(headers);
            segment.addOutboundRequestHeaders(outboundHeaders); // Step into wrapper class
        } catch (Throwable t) {
            reportInstrumentationError(t);
        }
    }

    /**
     * Report the external call with response information using reflection.
     */
    public static void reportAsExternal(Connection connection, URI requestURI, Segment segment) {
        if (segment == null || requestURI == null || connection == null) {
            return;
        }
        try {
            // Use reflection to get method(), status(), responseHeaders()
            Method methodMethod = connection.getClass().getMethod("method");
            methodMethod.setAccessible(true);
            Object httpMethod = methodMethod.invoke(connection);
            Method methodNameMethod = httpMethod.getClass().getMethod("name");
            methodNameMethod.setAccessible(true);
            String method = (String) methodNameMethod.invoke(httpMethod);

            Method statusMethod = connection.getClass().getMethod("status");
            statusMethod.setAccessible(true);
            Object httpStatus = statusMethod.invoke(connection);
            Method codeMethod = httpStatus.getClass().getMethod("code");
            codeMethod.setAccessible(true);
            int statusCode = (Integer) codeMethod.invoke(httpStatus);
            Method reasonPhraseMethod = httpStatus.getClass().getMethod("reasonPhrase");
            reasonPhraseMethod.setAccessible(true);
            String statusText = (String) reasonPhraseMethod.invoke(httpStatus);

            Method responseHeadersMethod = connection.getClass().getMethod("responseHeaders");
            responseHeadersMethod.setAccessible(true);
            Object responseHeaders = responseHeadersMethod.invoke(connection);

            InboundHeadersWrapper inboundHeaders = new InboundHeadersWrapper(responseHeaders);

            segment.reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(requestURI)
                    .procedure(method)
                    .inboundHeaders(inboundHeaders)
                    .status(statusCode, statusText)
                    .build());
        } catch (Throwable t) {
            reportInstrumentationError(t);
        }
    }

    private static void reportInstrumentationError(Throwable t) {
        AgentBridge.getAgent()
                .getLogger()
                .log(Level.FINEST, t, "Caught exception in Reactor-Netty instrumentation: {0}");
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
    }
}