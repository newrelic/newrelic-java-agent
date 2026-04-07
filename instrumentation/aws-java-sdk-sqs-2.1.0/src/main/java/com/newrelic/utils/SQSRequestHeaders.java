package com.newrelic.utils;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SQSRequestHeaders implements Headers {

    private SendMessageRequest request = null;
    private final Map<String, String> additionalAttributes = new Hashtable<String, String>();

    public SQSRequestHeaders(SendMessageRequest req) {
        request = req;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        Map<String, MessageAttributeValue> messageAttributes = request.messageAttributes();
        if (messageAttributes != null) {
            MessageAttributeValue value = messageAttributes.get(name);
            if (value != null && value.dataType().equalsIgnoreCase("string")) {
                return value.stringValue();
            }
        }

        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> list = new ArrayList<String>();
        String value = getHeader(name);
        if (value != null) {
            list.add(value);
        }
        return list;
    }

    @Override
    public void setHeader(String name, String value) {
        if (request != null) {
            Map<String, MessageAttributeValue> existingAttributes = request.messageAttributes();
            if (!existingAttributes.containsKey(name)) {
                additionalAttributes.put(name, value);
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        setHeader(name, value);
    }

    @Override
    public Collection<String> getHeaderNames() {
        Map<String, MessageAttributeValue> messageAttributes = request.messageAttributes();
        return messageAttributes.keySet();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeaderNames().contains(name);
    }

    public SendMessageRequest getUpdatedRequest() {
        SendMessageRequest.Builder builder = request.toBuilder();
        Map<String, MessageAttributeValue> msgAttributes = request.messageAttributes();

        Map<String, MessageAttributeValue> updatedAttributes = new HashMap<String, MessageAttributeValue>();

        if (msgAttributes != null && !msgAttributes.isEmpty()) {
            updatedAttributes.putAll(msgAttributes);
        }

        for (String key : additionalAttributes.keySet()) {
            MessageAttributeValue newValue = MessageAttributeValue.builder().dataType("String").stringValue(additionalAttributes.get(key)).build();
            updatedAttributes.put(key, newValue);
        }
        builder.messageAttributes(updatedAttributes);

        SendMessageRequest updated = builder.build();
        return updated;
    }
}
