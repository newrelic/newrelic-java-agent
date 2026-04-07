package com.nr.instrumentation.spring.kafka;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.Trace;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.messaging.handler.HandlerMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

// We just need the InstrumentationTestRunner so we can start and end transactions
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.apache.kafka", "org.springframework"}, configName="kafka-batch-enabled.yml")
public class SpringKafkaUtilTest {

    @Before
    public void setup() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        introspector.clear();
    }

    @Test
    public void testBatchTracingEnabled() {
        assertTrue(SpringKafkaUtil.DT_CONSUMER_BATCH_ENABLED);
    }

    @Test
    public void testNameHandlerFromMethod() throws NoSuchMethodException {
        runNameHandlerFromMethod();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<String> transactionNames = introspector.getTransactionNames();
        assertEquals(1, transactionNames.size());
        String actualTransactionName = transactionNames.iterator().next();
        final String expectedTransactionName =
                "OtherTransaction/Message/SpringKafka/com.nr.instrumentation.spring.kafka.SpringKafkaUtilTest.testNameHandlerFromMethod";
        assertEquals(expectedTransactionName, actualTransactionName);
    }

    @Trace(dispatcher = true)
    private void runNameHandlerFromMethod() throws NoSuchMethodException {
        HandlerMethod handlerMethod = new HandlerMethod(this.getClass(), this.getClass().getMethod("testNameHandlerFromMethod"));
        SpringKafkaUtil.nameTransactionFromMethod(handlerMethod);
    }

    @Test
    public void processMessageListenerConsumerRecord() {
        runProcessMessageListenerConsumerRecord();
        assertProcessMessageListener();
    }

    private void runProcessMessageListenerConsumerRecord() {
        ConsumerRecord<String, String> record = createConsumerRecord();
        runProcessMessageListenerTxn(record);
    }

    @Test
    public void processMessageListenerConsumerRecords() {
        runProcessMessageListenerConsumerRecords();
        assertProcessMessageListener();
    }

    private void runProcessMessageListenerConsumerRecords() {
        ConsumerRecord<String, String> record = createConsumerRecord();

        List<ConsumerRecord<String, String>> recordList = new ArrayList<>();
        recordList.add(record);

        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordMap = new HashMap<>();
        recordMap.put(new TopicPartition(record.topic(), record.partition()), recordList);

        ConsumerRecords<String, String> consumerRecords = new ConsumerRecords<>(recordMap);

        runProcessMessageListenerTxn(consumerRecords);
    }

    @Test
    public void processMessageListenerConsumerRecordList() {
        runProcessMessageListenerConsumerRecordList();
        assertProcessMessageListener();
    }

    private void runProcessMessageListenerConsumerRecordList() {
        ConsumerRecord<String, String> record = createConsumerRecord();

        List<ConsumerRecord<String, String>> recordList = new ArrayList<>();
        recordList.add(record);

        runProcessMessageListenerTxn(recordList);
    }

    private void assertProcessMessageListener() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<String> transactionNames = introspector.getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txnName = transactionNames.iterator().next();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txnName);
        TransactionTrace trace = traces.iterator().next();
        TraceSegment rootSegment = trace.getInitialTraceSegment();
        assertEquals("MessageBroker/SpringKafka/Topic/Consume/Named/my-topic", rootSegment.getName());
        assertEquals(1, rootSegment.getChildren().size());
        TraceSegment childSegment = rootSegment.getChildren().iterator().next();
        assertNotEquals("MessageBroker/SpringKafka/Topic/Consume/Named/my-topic", childSegment.getName());
    }

    @Trace(dispatcher = true)
    public <T> void runProcessMessageListenerTxn(T data) {
        SpringKafkaUtil.processMessageListener(data);
        runProcessMessageListenerNext(data);
    }

    @Trace(dispatcher = true)
    public <T> void runProcessMessageListenerNext(T data) {
        SpringKafkaUtil.processMessageListener(data);
    }

    private ConsumerRecord<String, String> createConsumerRecord() {
        return new ConsumerRecord<>("my-topic", 1, 1, "my-key", "my-value");
    }
}
