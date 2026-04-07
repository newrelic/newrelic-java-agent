package com.newrelic.utils;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SQSBatchRequestHeaders implements Headers {

    private SendMessageBatchRequestEntry requestEntry = null;
    private final Map<String, String> additionalAttributes = new Hashtable<String, String>();

    public SQSBatchRequestHeaders(SendMessageBatchRequestEntry re) {
        requestEntry = re;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        Map<String, MessageAttributeValue> attributes = requestEntry.messageAttributes();
        if (attributes != null) {
            MessageAttributeValue value = attributes.get(name);
            if (value != null) {
                String dataType = value.dataType();
                if (dataType.equalsIgnoreCase("String")) {
                    return value.stringValue();
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
            if (!requestEntry.messageAttributes().containsKey(name)) {
                additionalAttributes.put(name, value);
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (requestEntry != null) {
            if (!requestEntry.messageAttributes().containsKey(name)) {
                additionalAttributes.put(name, value);
            }
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        if (requestEntry != null) {
            Map<String, MessageAttributeValue> attributes = requestEntry.messageAttributes();
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

    public SendMessageBatchRequestEntry updatedEntry() {
        SendMessageBatchRequestEntry.Builder builder = requestEntry.toBuilder();
        Map<String, MessageAttributeValue> msgAttributes = requestEntry.messageAttributes();

        Map<String, MessageAttributeValue> updatedAttributes = new HashMap<String, MessageAttributeValue>();

        if (msgAttributes != null && !msgAttributes.isEmpty()) {
            updatedAttributes.putAll(msgAttributes);
        }

        for (String key : additionalAttributes.keySet()) {
            MessageAttributeValue newValue = MessageAttributeValue.builder().dataType("String").stringValue(additionalAttributes.get(key)).build();
            updatedAttributes.put(key, newValue);
        }
        builder.messageAttributes(updatedAttributes);

        SendMessageBatchRequestEntry updated = builder.build();

        return updated;
    }
}
