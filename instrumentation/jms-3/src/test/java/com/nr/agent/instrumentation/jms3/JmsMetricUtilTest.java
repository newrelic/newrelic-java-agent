/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.TransactionNamePriority;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.Topic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.Vector;

import static org.junit.Assert.assertEquals;

public class JmsMetricUtilTest {
    PrivateApi mockPrivateApi;
    Transaction mockTransaction = Mockito.mock(Transaction.class);

    @Before
    public void init() throws Exception {
        mockPrivateApi = Mockito.mock(PrivateApi.class);
        AgentBridge.privateApi = mockPrivateApi;

        com.newrelic.agent.bridge.Agent mockAgent = Mockito.mock(com.newrelic.agent.bridge.Agent.class);
        AgentBridge.agent = mockAgent;
        Mockito.when(mockAgent.getTransaction()).thenReturn(mockTransaction);
        Mockito.when(mockAgent.getLogger()).thenReturn(Mockito.mock(Logger.class));

    }

    @Test
    public void testNameTransactionNullMessage() throws Exception {
        Message response = JmsMetricUtil.nameTransaction(null);
        assertEquals(null, response);
        Mockito.verify(mockTransaction, Mockito.times(1)).ignore();
    }

    @Test
    public void testNameTransactionWithMessageNeitherQueueNorTopic() throws Exception {
        // method under test fails because we have not mocked up proper contents of msg.getJMSDestination();
        // we are making sure that failure happens and is logged.
        Message mockMessage = Mockito.mock(Message.class);
        Message response = JmsMetricUtil.nameTransaction(mockMessage);
        assertEquals(mockMessage, response);
        Mockito.verify(mockTransaction, Mockito.times(0)).ignore();
        // The logger is a now a static field on the AgentBridge which is difficult to mock because it's final.
        // This makes the test not very useful, but oh well.
        // Mockito.verify(mockPrivateApi, Mockito.times(1)).log(Mockito.any(Level.class), Mockito.anyString());
    }

    @Test
    public void testNameTransactionTempQueue() throws Exception {
        Message mockMessage = Mockito.mock(Message.class);
        Queue q = Mockito.mock(TemporaryQueue.class);
        Mockito.when(mockMessage.getJMSDestination()).thenReturn(q);

        Message response = JmsMetricUtil.nameTransaction(mockMessage);
        assertEquals(mockMessage, response);
        Mockito.verify(mockTransaction, Mockito.times(0)).ignore();
        Mockito.verify(mockTransaction, Mockito.times(1)).setTransactionName(
                Mockito.eq(TransactionNamePriority.FRAMEWORK_LOW), Mockito.eq(false), Mockito.matches("Message"),
                Mockito.matches("JMS/Queue/Temp"));
    }

    @Test
    public void testNameTransactionNamedQueue() throws Exception {
        Message mockMessage = Mockito.mock(Message.class);
        Queue q = Mockito.mock(Queue.class);
        Mockito.when(q.getQueueName()).thenReturn("NamedQueue12345");
        Mockito.when(mockMessage.getJMSDestination()).thenReturn(q);

        Message response = JmsMetricUtil.nameTransaction(mockMessage);
        assertEquals(mockMessage, response);
        Mockito.verify(mockTransaction, Mockito.times(0)).ignore();
        Mockito.verify(mockTransaction, Mockito.times(1)).setTransactionName(
                Mockito.eq(TransactionNamePriority.FRAMEWORK_HIGH), Mockito.eq(false), Mockito.matches("Message"),
                Mockito.matches("JMS/Queue/Named"), Mockito.matches("NamedQueue12345"));
    }

    @Test
    public void testNameTransactionTempTopic() throws Exception {
        Message mockMessage = Mockito.mock(Message.class);
        Topic t = Mockito.mock(TemporaryTopic.class);
        Mockito.when(mockMessage.getJMSDestination()).thenReturn(t);

        Message response = JmsMetricUtil.nameTransaction(mockMessage);
        assertEquals(mockMessage, response);
        Mockito.verify(mockTransaction, Mockito.times(0)).ignore();
        Mockito.verify(mockTransaction, Mockito.times(1)).setTransactionName(
                Mockito.eq(TransactionNamePriority.FRAMEWORK_LOW), Mockito.eq(false), Mockito.matches("Message"),
                Mockito.matches("JMS/Topic/Temp"));
    }

    @Test
    public void testNameNormalization() {
        String transactionName;

        transactionName = JmsMetricUtil.normalizeName("someName");
        assertEquals("someName", transactionName);

        transactionName = JmsMetricUtil.normalizeName("someName." + UUID.randomUUID().toString());
        assertEquals("someName.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("$TMP$.esb-prod-log01.5AEB58043FBF56D0F0.206");
        assertEquals("$TMP$.esb-prod-log01.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("BETPAYOUT.BETSUMMARY.791089418");
        assertEquals("BETPAYOUT.BETSUMMARY.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("worker.heartbeat.1-prod-iqr-workers-05r:e96795bd-925c-41aa-81c4-cb064448f927");
        assertEquals("worker.heartbeat.1-prod-iqr-workers-05r:#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("$TMP$.ESB-P-DC1-SEC1_EC1.361E5840873E2616FE7.1");
        assertEquals("$TMP$.ESB-P-DC1-SEC1_#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("IBUS_CONTAINER.STATE_EVENT.bf3d11cf-0dd6-43d1-927d-53db539247b2");
        assertEquals("IBUS_CONTAINER.STATE_EVENT.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("deviceAck.0001384907e338f648eee5df1c59d0f3");
        assertEquals("deviceAck.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("events.1000014.store-customer-created");
        assertEquals("events.#.store-customer-created", transactionName);

        transactionName = JmsMetricUtil.normalizeName("2016.07.26.2.05.46.3");
        assertEquals("#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("10047");
        assertEquals("#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("fan.1001733.register");
        assertEquals("fan.#.register", transactionName);

        transactionName = JmsMetricUtil.normalizeName("NamedQueue12345");
        assertEquals("NamedQueue12345", transactionName);

        transactionName = JmsMetricUtil.normalizeName("doctena.country.185a3bc8-ab62-40b0-ab9e-889f6970b741.sync");
        assertEquals("doctena.country.#.sync", transactionName);

        transactionName = JmsMetricUtil.normalizeName("tflt.raw.am53.mes.stsmux.ServiceStatusReceived.10160538");
        assertEquals("tflt.raw.am53.mes.stsmux.ServiceStatusReceived.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("signal.trade.process.r:57df42d85f0000cc03993989:u:57dc0dc79be83e2bfc43d83d.4");
        assertEquals("signal.trade.process.r:#:u:#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("ENTITY.EVENT_MARKETS_STATE.2842809.1474928982361");
        assertEquals("ENTITY.EVENT_MARKETS_STATE.#", transactionName);

        transactionName = JmsMetricUtil.normalizeName("signal.received-one.1.5718f2c3480000150024346a.57b35ada283db42d2554e155");
        assertEquals("signal.received-one.#", transactionName);
    }

    @Test
    public void testNameTransactionNamedTopic() throws Exception {
        Message mockMessage = Mockito.mock(Message.class);
        Topic t = Mockito.mock(Topic.class);
        Mockito.when(t.getTopicName()).thenReturn("NamedTopic12345");
        Mockito.when(mockMessage.getJMSDestination()).thenReturn(t);

        Message response = JmsMetricUtil.nameTransaction(mockMessage);
        assertEquals(mockMessage, response);
        Mockito.verify(mockTransaction, Mockito.times(0)).ignore();
        Mockito.verify(mockTransaction, Mockito.times(1)).setTransactionName(
                Mockito.eq(TransactionNamePriority.FRAMEWORK_HIGH), Mockito.eq(false), Mockito.matches("Message"),
                Mockito.matches("JMS/Topic/Named"), Mockito.matches("NamedTopic12345"));
    }

    @Test
    public void testGetMessageParameters() throws Exception {

        final Map<String, Object> parms = new HashMap<>();
        parms.put("number", 42L);
        parms.put("string", "Hello World");
        parms.put("boolean", Boolean.TRUE);
        Date date = new Date();
        parms.put("date", date);
        parms.put("null", null);

        Message mockMessage = makeMessageWithParms(parms);

        Map<String, String> response = JmsMetricUtil.getMessageParameters(mockMessage);
        for (Entry<String, Object> entry : parms.entrySet()) {
            Object parm = parms.get(entry);
            assertEquals((parm == null) ? parm : parm.toString(), response.get(entry));
        }
    }

    private Message makeMessageWithParms(final Map<String, Object> parms) throws JMSException {
        Message mockMessage = Mockito.mock(Message.class);
        Vector<String> v = new Vector<>();
        for (String k : parms.keySet()) {
            v.add(k);
        }
        Mockito.when(mockMessage.getPropertyNames()).thenReturn(v.elements());
        Mockito.when(mockMessage.getObjectProperty(Mockito.anyString())).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return parms.get(args[0]);
            }
        });
        return mockMessage;
    }
}
