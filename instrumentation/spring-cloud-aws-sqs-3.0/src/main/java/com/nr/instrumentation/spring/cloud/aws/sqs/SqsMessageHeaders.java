/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring.cloud.aws.sqs;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Headers wrapper for Spring Cloud AWS SQS message attributes.
 * Handles W3C trace propagation through SQS message attributes.
 */
public class SqsMessageHeaders implements Headers {

    private final Message<?> springMessage;
    private final Map<String, MessageAttributeValue> messageAttributes;

    public SqsMessageHeaders(Message<?> springMessage, Map<String, MessageAttributeValue> messageAttributes) {
        this.springMessage = springMessage;
        this.messageAttributes = messageAttributes;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        // First check Spring message headers
        if (springMessage != null && springMessage.getHeaders().containsKey(name)) {
            Object headerValue = springMessage.getHeaders().get(name);
            return headerValue != null ? headerValue.toString() : null;
        }
        
        // Then check SQS message attributes
        if (messageAttributes != null && messageAttributes.containsKey(name)) {
            MessageAttributeValue attributeValue = messageAttributes.get(name);
            return attributeValue != null ? attributeValue.stringValue() : null;
        }
        
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        Collection<String> headers = new ArrayList<>();
        String headerValue = getHeader(name);
        if (headerValue != null) {
            headers.add(headerValue);
        }
        return headers;
    }

    @Override
    public void setHeader(String name, String value) {
        // For outbound messages, we need to modify message attributes
        if (messageAttributes != null) {
            messageAttributes.put(name, MessageAttributeValue.builder()
                    .stringValue(value)
                    .dataType("String")
                    .build());
        }
    }

    @Override
    public void addHeader(String name, String value) {
        setHeader(name, value);
    }

    @Override
    public Collection<String> getHeaderNames() {
        Collection<String> headerNames = new ArrayList<>();
        
        // Add Spring message header names
        if (springMessage != null) {
            headerNames.addAll(springMessage.getHeaders().keySet());
        }
        
        // Add SQS message attribute names
        if (messageAttributes != null) {
            headerNames.addAll(messageAttributes.keySet());
        }
        
        return headerNames;
    }

    @Override
    public boolean containsHeader(String name) {
        if (springMessage != null && springMessage.getHeaders().containsKey(name)) {
            return true;
        }
        
        return messageAttributes != null && messageAttributes.containsKey(name);
    }
}