package com.newrelic.utils;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SQSBatchRequestHeaders implements Headers {
    private final SendMessageBatchRequestEntry requestEntry;

    public SQSBatchRequestHeaders(SendMessageBatchRequestEntry re) {
        requestEntry = re;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        Map<String, MessageAttributeValue> attributes = requestEntry.getMessageAttributes();
        if (attributes != null) {
            MessageAttributeValue value = attributes.get(name);
            if (value != null) {
                String dataType = value.getDataType();
                if (dataType.equalsIgnoreCase("String")) {
                    String stringValue = value.getStringValue();
                    if (stringValue != null) {
                        return stringValue;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> list = new ArrayList<String>();
        String value = getHeader(name);
        if (value != null && !value.isEmpty()) {
            list.add(value);
        }
        return list;
    }

    @Override
    public void setHeader(String name, String value) {
        if (requestEntry != null) {
            requestEntry.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (requestEntry != null) {
            requestEntry.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        if (requestEntry != null) {
            Map<String, MessageAttributeValue> attributes = requestEntry.getMessageAttributes();
            if (attributes != null) {
                return attributes.keySet();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeaderNames().contains(name);
    }
}
