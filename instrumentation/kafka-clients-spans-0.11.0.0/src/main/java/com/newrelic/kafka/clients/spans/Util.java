package com.newrelic.kafka.clients.spans;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.HeaderType;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;

public class Util {
    private static final String W3C_TRACE_PARENT = "traceparent";
    private static final String W3C_TRACE_STATE = "tracestate";

    private static final String NR_HEADER_NAME = "newrelic";

    public Util() {}

    public static void passDTHeaders(ProducerRecord record) {
        final Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            ConcurrentHashMapHeaders dtHeaders = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);
            transaction.insertDistributedTraceHeaders(dtHeaders);

            // Clean headers to prevent duplicate records and maintain a consistent state

            if (dtHeaders.containsHeader(W3C_TRACE_PARENT)) {
                record.headers().remove(W3C_TRACE_PARENT);
                record.headers().add(W3C_TRACE_PARENT, dtHeaders.getHeader(W3C_TRACE_PARENT).getBytes(StandardCharsets.UTF_8));
            }

            if (dtHeaders.containsHeader(W3C_TRACE_STATE)) {
                record.headers().remove(W3C_TRACE_STATE);
                record.headers().add(W3C_TRACE_STATE, dtHeaders.getHeader(W3C_TRACE_STATE).getBytes(StandardCharsets.UTF_8));
            }

            if (dtHeaders.containsHeader(NR_HEADER_NAME)) {
                record.headers().remove(NR_HEADER_NAME);
                record.headers().add(NR_HEADER_NAME, dtHeaders.getHeader(NR_HEADER_NAME).getBytes(StandardCharsets.UTF_8));
            }

        }
    }
}
