package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.api.agent.Trace;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.camel")
public class KafkaExchangeProcessorTest {

    @Before
    public void setup() {
        InstrumentationTestRunner.getIntrospector().clear();
    }

    @Test
    public void kafkaExchangeProcessorAllowsTxns() {
        Assert.assertTrue(new KafkaExchangeProcessor().shouldStartTransaction());
    }

    @Test
    public void kafkaExchangeProcessorNameTxn() {
        runTxn();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        Assert.assertEquals(1, introspector.getTransactionNames().size());
        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Message/Kafka/Topic/Consume/Named/topicA");
        Assert.assertEquals(1, events.size());
    }

    @Trace(dispatcher = true)
    public void runTxn() {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        Map<String, Object> headers = new HashMap<>();
        headers.put("h1", "v2");

        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Endpoint endpoint = Mockito.mock(Endpoint.class);

        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(exchange.getIn().getHeaders()).thenReturn(headers);
        Mockito.when(message.getBody()).thenReturn("");
        Mockito.when(endpoint.getEndpointUri()).thenReturn("kafka:topicA?brokers=localhost:9092");
        Mockito.when(exchange.getFromEndpoint()).thenReturn(endpoint);

        ExchangeProcessor processor = new KafkaExchangeProcessor();
        processor.nameTransaction(transaction, exchange);
    }

    @Test
    public void kafkaExchangeProcessorNameTxnBatch() {
        runTxnBatch();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        Assert.assertEquals(1, introspector.getTransactionNames().size());
        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Message/Kafka/Topic/Consume/Named/batchTopicA");
        Assert.assertEquals(1, events.size());
    }

    @Trace(dispatcher = true)
    public void runTxnBatch() {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        Map<String, Object> headers = new HashMap<>();
        headers.put("h1", "v2");

        Exchange childExchange = Mockito.mock(Exchange.class);
        Message childMessage = Mockito.mock(Message.class);
        Endpoint endpoint = Mockito.mock(Endpoint.class);


        Mockito.when(endpoint.getEndpointUri()).thenReturn("kafka:batchTopicA?brokers=localhost:9092");

        Mockito.when(childMessage.getBody()).thenReturn("");
        Mockito.when(childExchange.getIn()).thenReturn(childMessage);
        Mockito.when(childExchange.getIn().getHeaders()).thenReturn(headers);
        Mockito.when(childExchange.getFromEndpoint()).thenReturn(endpoint);


        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);

        Mockito.when(message.getBody()).thenReturn(Collections.singletonList(childExchange));
        Mockito.when(message.getBody(List.class)).thenReturn(Collections.singletonList(childExchange));

        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(exchange.getFromEndpoint()).thenReturn(null);


        ExchangeProcessor processor = new KafkaExchangeProcessor();
        processor.nameTransaction(transaction, exchange);
    }
}
