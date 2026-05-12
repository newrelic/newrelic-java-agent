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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collection;


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
        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Message/Kafka/Consume/Topic/Named/topicA");
        Assert.assertEquals(1, events.size());
    }

    @Trace(dispatcher = true)
    public void runTxn() {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        Exchange exchange = Mockito.mock(Exchange.class);
        Endpoint endpoint = Mockito.mock(Endpoint.class);
        Mockito.when(endpoint.getEndpointUri()).thenReturn("kafka:topicA?brokers=localhost:9092");
        Mockito.when(exchange.getFromEndpoint()).thenReturn(endpoint);

        new DefaultExchangeProcessor().nameTransaction(transaction, exchange);
    }
}
