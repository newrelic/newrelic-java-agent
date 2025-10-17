package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.model.SpanEvent;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SpanEventImplTest {


    @Test
    public void getters_returnUnderlyingSpanEventData() throws IOException {
        SpanEvent.Builder spanEventBuilder = SpanEvent.builder();
        SpanEvent spanEvent = spanEventBuilder.appName("appname").priority(.1f).timestamp(100000).putIntrinsic("category", "http")
                .putIntrinsic("transactionId", "transactionid").putIntrinsic("duration", 0.1f)
                .putIntrinsic("name", "name").putIntrinsic("traceId", "traceid").putIntrinsic("guid", "guid")
                .putIntrinsic("parentId", "parentid").putAgentAttribute("http.url", "url").putAgentAttribute("http.statusCode", 200)
                .putIntrinsic("component", "component").putAgentAttribute("http.statusText", "statustext")
                .putAgentAttribute("http.method", "method").build();
        com.newrelic.agent.introspec.SpanEvent instance = new SpanEventImpl(spanEvent);

        assertEquals("name", instance.getName());
        assertEquals(.1f, instance.duration(), 0);
        assertEquals("traceid", instance.traceId());
        assertEquals("parentid", instance.parentId());
        assertEquals("http", instance.category());
        assertEquals("name", instance.getName());
        assertEquals("url", instance.getHttpUrl());
        assertEquals("method", instance.getHttpMethod());
        assertEquals("component", instance.getHttpComponent());
        assertEquals("transactionid", instance.getTransactionId());
        assertEquals(new Integer(200), instance.getStatusCode());
        assertEquals("statustext", instance.getStatusText());
        assertEquals(4, instance.getAgentAttributes().size());
    }


}
