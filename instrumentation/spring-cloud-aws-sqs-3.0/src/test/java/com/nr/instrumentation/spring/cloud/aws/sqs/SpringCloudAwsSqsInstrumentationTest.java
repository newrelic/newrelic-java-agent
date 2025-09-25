/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring.cloud.aws.sqs;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TracedMetricData;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"io.awspring.cloud.sqs", "com.nr.instrumentation.spring.cloud.aws.sqs"})
public class SpringCloudAwsSqsInstrumentationTest {

    private static final String LIBRARY_NAME = "SpringCloudAwsSqs";
    private static final String QUEUE_NAME = "test-queue";

    private AnnotationConfigApplicationContext context;
    private SqsTemplate sqsTemplate;

    @Configuration
    static class TestConfiguration {
        @Bean
        public SqsAsyncClient sqsAsyncClient() {
            return SqsAsyncClient.builder()
                    .endpointOverride(URI.create("http://localhost:9324"))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                    .region(Region.US_EAST_1)
                    .build();
        }

        @Bean
        public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
            return SqsTemplate.newTemplate(sqsAsyncClient);
        }
    }

    @Before
    public void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        sqsTemplate = context.getBean(SqsTemplate.class);
    }

    @Test
    public void testUtilExtractQueueNameFromUrl() {
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue";
        String queueName = SpringCloudAwsSqsUtil.extractQueueNameFromUrl(queueUrl);
        assertEquals("my-queue", queueName);
        
        // Test with simple queue name
        String simpleName = SpringCloudAwsSqsUtil.extractQueueNameFromUrl("simple-queue");
        assertEquals("simple-queue", simpleName);
    }

    @Test
    public void testSqsMessageHeadersWrapper() {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("testKey", MessageAttributeValue.builder().stringValue("testValue").build());
        
        SqsMessageHeaders headers = new SqsMessageHeaders(null, attributes);
        
        assertEquals("testValue", headers.getHeader("testKey"));
        assertTrue(headers.containsHeader("testKey"));
        
        // Test setting headers
        headers.setHeader("newKey", "newValue");
        assertEquals("newValue", headers.getHeader("newKey"));
    }

    @Test
    public void testProducerInstrumentation() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        introspector.clear();

        try {
            // This will fail in test since we don't have real SQS, but should trigger instrumentation
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/" + QUEUE_NAME)
                    .messageBody("Test message")
                    .build();
            
            sqsTemplate.send(request);
        } catch (Exception e) {
            // Expected to fail in test environment
        }

        // Verify that the transaction was created (even if it failed)
        assertEquals(1, introspector.getFinishedTransactionCount());
        
        String transactionName = introspector.getTransactionNames().iterator().next();
        assertNotNull(transactionName);
    }
}