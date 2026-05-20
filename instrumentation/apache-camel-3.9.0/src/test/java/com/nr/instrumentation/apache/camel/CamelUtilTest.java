/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.apache.camel;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.nr.instrumentation.apache.camel.processors.DefaultExchangeProcessor;
import com.nr.instrumentation.apache.camel.processors.ExchangeProcessor;
import com.nr.instrumentation.apache.camel.processors.KafkaExchangeProcessor;
import com.nr.instrumentation.apache.camel.processors.NoOpExchangeProcessor;
import org.apache.camel.Endpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.camel")
public class CamelUtilTest {

    @Test
    public void endpointOperation() throws IOException {
        String url = "kafka:topicA?brokers=localhost:9092";
        try (Endpoint endpoint = mockEndpoint(url)) {
            String operation = CamelUtil.endpointOperation(endpoint);
            assertEquals("kafka", operation);
        }
    }

    @Test
    public void testKafkaProcessor() throws IOException {
        List<String> urls = new ArrayList<>();
        urls.add("kafka:topicB?brokers=localhost:9092");
        for (String url : urls) {
            try (Endpoint endpoint = mockEndpoint(url)) {
                ExchangeProcessor processor = CamelUtil.getExchangeProcessor(endpoint);
                boolean isCorrectProcessor = processor instanceof KafkaExchangeProcessor;
                assertTrue("Exchange processor of class " + processor.getClass().getName() + " is expected to be KafkaExchangeProcessor for url " + url,
                        isCorrectProcessor);
            }

        }
    }

    @Test
    public void testNoOpProcessor() throws IOException {
        List<String> urls = new ArrayList<>();
        urls.add("direct:contents?param=value");
        urls.add("direct-vm:contents?param=value");
        urls.add("disruptor:contents?param=value");
        urls.add("disruptor-vm:contents?param=value");
        urls.add("log:contents?param=value");
        urls.add("seda:contents?param=value");
        urls.add("seda-vm:contents?param=value");
        for (String url : urls) {
            try (Endpoint endpoint = mockEndpoint(url)) {
                ExchangeProcessor processor = CamelUtil.getExchangeProcessor(endpoint);
                boolean isCorrectProcessor = processor instanceof NoOpExchangeProcessor;
                assertTrue("Exchange processor of class " + processor.getClass().getName() + " is expected to be NoOpExchangeProcessor for url " + url,
                        isCorrectProcessor);
            }

        }
    }

    @Test
    public void testDefaultProcessor() throws IOException {
        List<String> urls = new ArrayList<>();
        urls.add("a:contents?param=value");
        urls.add("b:contents?param=value");
        for (String url : urls) {
            try (Endpoint endpoint = mockEndpoint(url)) {
                ExchangeProcessor processor = CamelUtil.getExchangeProcessor(endpoint);
                boolean isCorrectProcessor = processor instanceof DefaultExchangeProcessor;
                assertTrue("Exchange processor of class " + processor.getClass().getName() +
                        " is expected to be DefaultExchangeProcessor for url " + url, isCorrectProcessor);
            }

        }
    }

    private Endpoint mockEndpoint(String url) {
        Endpoint endpoint = Mockito.mock(Endpoint.class);
        Mockito.when(endpoint.getEndpointUri()).thenReturn(url);
        return endpoint;
    }



}
