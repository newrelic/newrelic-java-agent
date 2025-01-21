package com.newrelic.utils;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SQSRequestHeaders implements Headers {
    private final SendMessageRequest request;

    public SQSRequestHeaders(SendMessageRequest req) {
        request = req;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        Map<String, MessageAttributeValue> messageAttributes = request.getMessageAttributes();
        if (messageAttributes != null) {
            MessageAttributeValue value = messageAttributes.get(name);
            if (value != null && value.getDataType().equalsIgnoreCase("string")) {
                return value.getStringValue();
            }
        }
        Map<String, String> customRequestHeaders = request.getCustomRequestHeaders();
        if (customRequestHeaders != null) {
            return customRequestHeaders.get(name);
        }
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        String value = getHeader(name);
        if (value != null) {
            return Collections.singletonList(value);
        }
        return Collections.emptyList();
    }

    @Override
    public void setHeader(String name, String value) {
        if (request != null) {
            Map<String, MessageAttributeValue> existingAttributes = request.getMessageAttributes();
            if (!existingAttributes.containsKey(name)) {
                request.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (request != null) {
            Map<String, MessageAttributeValue> existingAttributes = request.getMessageAttributes();
            if (!existingAttributes.containsKey(name)) {
                request.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
            }
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        Map<String, MessageAttributeValue> messageAttributes = request.getMessageAttributes();
        return messageAttributes.keySet();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeaderNames().contains(name);
    }
}
