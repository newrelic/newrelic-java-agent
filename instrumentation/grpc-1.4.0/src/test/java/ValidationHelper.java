/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import app.TestServer;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

class ValidationHelper {

    static void validateGrpcInteraction(TestServer server, String clientTxName, String serverTxName, String fullMethod, String grpcType, String name) {
        checkTransactions(2);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String externalTxSegmentName = "ExternalTransaction/localhost/" + getCrossProcessId() + "/" + serverTxName;

        // Verify that CAT works
        CatHelper.verifyOneSuccessfulCat(introspector, clientTxName, serverTxName);

        // Client side
        Collection<TransactionTrace> clientTransactionTrace = introspector.getTransactionTracesForTransaction(clientTxName);
        assertEquals(1, clientTransactionTrace.size());
        TransactionTrace trace = clientTransactionTrace.iterator().next();
        boolean foundSegment = false;
        for (TraceSegment segment : trace.getInitialTraceSegment().getChildren()) {
            // Verify external request exists and is valid
            if (segment.getClassName().equals("External")) {
                assertEquals(externalTxSegmentName, segment.getName());
                assertEquals("grpc://localhost:" + server.getPort() + "/" + fullMethod, segment.getUri()); // This is the value of "http.url"
                assertEquals(1, segment.getCallCount());
                assertEquals("", segment.getMethodName());
                assertEquals(grpcType, segment.getTracerAttributes().get("grpc.type"));
                assertEquals("gRPC", segment.getTracerAttributes().get("component"));
                assertEquals(fullMethod, segment.getTracerAttributes().get("http.method"));
                foundSegment = true;
            }
        }
        assertTrue("Unable to find client side External/ segment", foundSegment);

        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(clientTxName);
        assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals(Integer.valueOf(0), externalRequest.getStatusCode());
        assertNull(externalRequest.getStatusText());

        // Server side
        Collection<TransactionTrace> serverTransactionTrace = introspector.getTransactionTracesForTransaction(serverTxName);
        assertEquals(1, serverTransactionTrace.size());
        TransactionTrace serverTrace = serverTransactionTrace.iterator().next();
        TraceSegment rootSegment = serverTrace.getInitialTraceSegment();
        assertTrue(rootSegment.getName().endsWith(fullMethod));
        assertEquals(1, rootSegment.getCallCount());
        assertEquals(fullMethod, rootSegment.getTracerAttributes().get("request.method"));
        assertEquals(0, rootSegment.getTracerAttributes().get("response.status"));
        assertEquals(0, rootSegment.getTracerAttributes().get("http.statusCode"));
        assertNull(rootSegment.getTracerAttributes().get("http.statusText"));
        assertEquals(grpcType, rootSegment.getTracerAttributes().get("grpc.type"));

        // Custom attributes (to test tracing into customer code)
        Collection<TransactionEvent> serverTxEvents = introspector.getTransactionEvents(serverTxName);
        assertEquals(1, serverTxEvents.size());
        TransactionEvent serverTxEvent = serverTxEvents.iterator().next();
        assertNotNull(serverTxEvent);
        if (grpcType.equals("BIDI_STREAMING")) {
            // For the streaming case, we just ensure that the transaction is available to use for a streaming handler but we do not propagate it
            assertEquals("true", serverTxEvent.getAttributes().get("customParameter"));
        } else {
            assertEquals(name, serverTxEvent.getAttributes().get("sayHelloBefore"));
            assertEquals(name, serverTxEvent.getAttributes().get("sayHelloAfter"));
        }

        assertEquals(0, serverTxEvent.getAttributes().get("response.status"));
        assertEquals(0, serverTxEvent.getAttributes().get(AttributeNames.HTTP_STATUS_CODE));
        assertEquals("grpc://localhost:" + server.getPort() + "/" + fullMethod, serverTxEvent.getAttributes().get("request.uri"));
    }

    static void validateExceptionGrpcInteraction(TestServer server, String clientTxName, String serverTxName, String fullMethod, String grpcType, String name,
            int status) {
        checkTransactions(2);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String externalTxSegmentName = "ExternalTransaction/localhost/" + getCrossProcessId() + "/" + serverTxName;

        // Client side
        Collection<TransactionTrace> clientTransactionTrace = introspector.getTransactionTracesForTransaction(clientTxName);
        assertEquals(1, clientTransactionTrace.size());
        TransactionTrace trace = clientTransactionTrace.iterator().next();
        boolean foundSegment = false;
        for (TraceSegment segment : trace.getInitialTraceSegment().getChildren()) {
            // Verify external request exists and is valid
            if (segment.getClassName().equals("External")) {
                assertEquals(externalTxSegmentName, segment.getName());
                assertEquals("grpc://localhost:" + server.getPort() + "/" + fullMethod, segment.getUri());
                assertEquals(1, segment.getCallCount());
                assertEquals("", segment.getMethodName());
                assertEquals(grpcType, segment.getTracerAttributes().get("grpc.type"));
                assertEquals("gRPC", segment.getTracerAttributes().get("component"));
                assertEquals(fullMethod, segment.getTracerAttributes().get("http.method"));
                foundSegment = true;
            }
        }
        assertTrue("Unable to find client side External/ segment", foundSegment);

        // Server side
        Collection<TransactionTrace> serverTransactionTrace = introspector.getTransactionTracesForTransaction(serverTxName);
        assertEquals(1, serverTransactionTrace.size());
        TransactionTrace serverTrace = serverTransactionTrace.iterator().next();
        TraceSegment rootSegment = serverTrace.getInitialTraceSegment();
        assertTrue(rootSegment.getName().endsWith(fullMethod));
        assertEquals(1, rootSegment.getCallCount());
        assertEquals(fullMethod, rootSegment.getTracerAttributes().get("request.method"));
        assertEquals(status, rootSegment.getTracerAttributes().get("response.status"));
        assertEquals(status, rootSegment.getTracerAttributes().get(AttributeNames.HTTP_STATUS_CODE));
        assertEquals(grpcType, rootSegment.getTracerAttributes().get("grpc.type"));

        // Custom attributes (to test tracing into customer code)
        Collection<TransactionEvent> serverTxEvents = introspector.getTransactionEvents(serverTxName);
        assertEquals(1, serverTxEvents.size());
        TransactionEvent serverTxEvent = serverTxEvents.iterator().next();
        assertNotNull(serverTxEvent);
        assertEquals(status, serverTxEvent.getAttributes().get("response.status"));
        assertEquals(status, serverTxEvent.getAttributes().get(AttributeNames.HTTP_STATUS_CODE));
        assertEquals("grpc://localhost:" + server.getPort() + "/" + fullMethod, serverTxEvent.getAttributes().get("request.uri"));
    }

    private static void checkTransactions(int expected) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(expected, introspector.getFinishedTransactionCount(30000));
    }

    private static String getCrossProcessId() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getCrossProcessConfig().getCrossProcessId();
    }
}
