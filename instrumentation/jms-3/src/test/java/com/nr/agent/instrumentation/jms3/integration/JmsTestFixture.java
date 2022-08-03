/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3.integration;

import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * An instance of this class is constructed from a provider-specific connection factory implementation. The code in this
 * class is provider-agnostic.
 */
public class JmsTestFixture implements JmsProviderTest {
    private final ConnectionFactory factory;
    private final Params params;

    public static class Params {
        private final Map<String, Object> params;

        private Params(Map<String, Object> params) {
            this.params = params;
        }

        String getQueueName() {
            return (String) params.get("queueName");
        }

        String getSegmentName() {
            return (String) params.get("segmentName");
        }

        String getSegmentClassName() {
            return (String) params.get("segmentClassName");
        }

        String getSegmentMethodName() {
            return (String) params.get("segmentMethodName");
        }

        public static class Builder {
            private final Map<String, Object> params = new HashMap<>();

            public Params build() {
                return new Params(params);
            }

            // Mandatory - name of queue to be used for test
            public Builder addQueueName(String qn) {
                params.put("queueName", qn);
                return this;
            }

            // Mandatory - name of one segment that will appear in the responder's tx trace
            public Builder addSegmentName(String sn) {
                params.put("segmentName", sn);
                return this;
            }

            // Mandatory - class name found in the same named segment in the responder's tx trace
            public Builder addSegmentClassName(String cn) {
                params.put("segmentClassName", cn);
                return this;
            }

            // Mandatory - method name found in the same named responder segment
            public Builder addSegmentMethodName(String mn) {
                params.put("segmentMethodName", mn);
                return this;
            }
        }
    }

    public JmsTestFixture(ConnectionFactory factory, Params params) {
        this.factory = factory;
        this.params = params;
    }

    @Override
    public void testEchoServer() throws Exception {
        echoTest();
        echoTestAssertions();
    }

    // Implementation details to end of class...

    private final String REQUESTER_TX_NAME = "JmsEchoTest/Requester";
    private final String RESPONDER_TX_NAME = "JmsEchoTest/Responder";

    @Trace(dispatcher = true)
    private void echoTest() throws Exception {
        final String VALID_MESSAGE = "Request was successfully processed.";
        final String INVALID_MESSAGE = "Request has an unexpected message type";

        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", REQUESTER_TX_NAME);

        // Error handling is not quite correct here - won't call shutdown on first if second fails - oh well.
        final JmsTestSession serverSession = new JmsTestSession(factory, false, Session.AUTO_ACKNOWLEDGE, params.getQueueName());
        final JmsTestSession clientSession = new JmsTestSession(factory, false, Session.AUTO_ACKNOWLEDGE, params.getQueueName());

        try {
            final MessageProducer serverResponseProducer = serverSession.getProducer(null, DeliveryMode.NON_PERSISTENT);
            final MessageConsumer serverRequestConsumer = serverSession.getConsumer();
            serverRequestConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", RESPONDER_TX_NAME);

                        TextMessage response = serverSession.createTextMessage();
                        response.setText((message instanceof TextMessage) ? VALID_MESSAGE : INVALID_MESSAGE);
                        response.setJMSCorrelationID(message.getJMSCorrelationID());
                        serverResponseProducer.send(message.getJMSReplyTo(), response);
                    } catch (Exception e) {
                        // There's no easy way to have a side effect of this anonymous method (which is typically
                        // running on one of the JMS client implementation's pool threads) cause an immediate failure
                        // of the test. But if we ignore the transaction, multiple assertions should fail later.
                        NewRelic.getAgent().getTransaction().ignore();
                    }
                }
            });

            serverSession.start();
            clientSession.start();

            final MessageProducer clientRequestProducer = clientSession.getProducer(DeliveryMode.NON_PERSISTENT);
            final Destination tempDest = clientSession.getTemporaryQueue();
            final MessageConsumer clientResponseConsumer = clientSession.getConsumer(tempDest);
            final TextMessage request = clientSession.createTextMessage();
            final String correlationId = this.createRandomString();

            request.setText("This client request was generated " + new Date());
            request.setJMSReplyTo(tempDest);
            request.setJMSCorrelationID(correlationId);
            clientRequestProducer.send(request);

            final Message response = clientResponseConsumer.receive();
            if (!(response instanceof TextMessage) || !((TextMessage) response).getText().equals(VALID_MESSAGE)) {
                throw new RuntimeException("Invalid response type from JMS server");
            }

        } finally {
            serverSession.shutdown();
            clientSession.shutdown();
        }
    }

    private void echoTestAssertions() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(2, introspector.getFinishedTransactionCount());

        // Find the requester and responder transactions
        final String requesterFullTxName = String.format("OtherTransaction/%s/%s", "Custom", REQUESTER_TX_NAME);
        final String responderFullTxName = String.format("OtherTransaction/%s/%s", "Custom", RESPONDER_TX_NAME);

        Iterator<String> it = introspector.getTransactionNames().iterator();
        String txName1 = it.next();
        String txName2 = it.next();
        String requester = (txName1.contains(requesterFullTxName)) ? txName1 : txName2;
        String responder = (requester.equals(txName1)) ? txName2 : txName1;
        assertNotNull(requester);
        assertNotNull(responder);
        assertNotEquals(requester, responder);

        // scoped metrics

        assertEquals(1, MetricsHelper.getScopedMetricCount(requester, "Java/com.nr.agent.instrumentation.jms3.integration.JmsTestFixture/echoTest"));
        assertEquals(1, MetricsHelper.getScopedMetricCount(requester, "MessageBroker/JMS/Queue/Produce/Named/InstrumentationTestQueue"));
        // This produces a metric with a unique ID for the last segment each run: MessageBroker/JMS/Queue/Consume/Named/9cbf467e-51ad-47d9-958b-01c17c62e144
        assertTrue(MetricsHelper.matchingMetricExists(requester, "MessageBroker/JMS/Queue/Consume/Named/"));

        assertEquals(1, MetricsHelper.getScopedMetricCount(responder, "MessageBroker/JMS/Queue/Consume/Named/InstrumentationTestQueue"));
        assertEquals(1, MetricsHelper.getScopedMetricCount(responder, "MessageBroker/JMS/Queue/Produce/Temp"));

        // unscoped metrics

        assertEquals(1, MetricsHelper.getUnscopedMetricCount("OtherTransactionTotalTime/Custom/JmsEchoTest/Requester"));
        assertEquals(2, MetricsHelper.getUnscopedMetricCount("OtherTransaction/all"));
        assertEquals(2, MetricsHelper.getUnscopedMetricCount("OtherTransactionTotalTime"));

        // transaction events

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(requesterFullTxName);
        assertEquals(1, transactionEvents.size());

        transactionEvents = introspector.getTransactionEvents(responderFullTxName);
        assertEquals(1, transactionEvents.size());

        // Now dig out the transaction trace for the responder (arbitrary choice, could have pulled
        // out the requester transaction) and validate that there is one trace segment that matches
        // the parameters passed from the platform-specific test code.
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(responderFullTxName);
        assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        boolean matched = false;
        for (TraceSegment segment : trace.getInitialTraceSegment().getChildren()) {
            if (segment.getName().equals(params.getSegmentName())) {
                if (segment.getClassName().equals(params.getSegmentClassName()) && segment.getMethodName().equals(
                        params.getSegmentMethodName())) {
                    matched = true;
                }
            }
            assertTrue(segment.getTracerAttributes().containsKey("exclusive_duration_millis"));
        }
        assertTrue(matched);

        // Finally do a simpler verification on the requester trace.
        traces = introspector.getTransactionTracesForTransaction(responderFullTxName);
        assertEquals(1, traces.size());
    }

    private String createRandomString() {
        Random random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }

}
