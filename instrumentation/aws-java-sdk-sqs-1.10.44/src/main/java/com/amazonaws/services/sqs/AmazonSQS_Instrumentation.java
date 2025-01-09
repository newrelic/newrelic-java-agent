/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.sqs;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.SqsV1Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.sqs.AmazonSQS")
public class AmazonSQS_Instrumentation {

    @Trace
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        for (SendMessageBatchRequestEntry request : sendMessageBatchRequest.getEntries()) {
            SQSBatchRequestHeaders headers = new SQSBatchRequestHeaders(request);
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        }

        MessageProduceParameters messageProduceParameters = SqsV1Util.generateExternalProduceMetrics(sendMessageBatchRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    @Trace
    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
        SQSRequestHeaders headers = new SQSRequestHeaders(sendMessageRequest);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        MessageProduceParameters messageProduceParameters = SqsV1Util.generateExternalProduceMetrics(sendMessageRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    @Trace
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        List<String> updatedMessageAttrNames = new ArrayList<>(receiveMessageRequest.getMessageAttributeNames());
        Collections.addAll(updatedMessageAttrNames, SqsV1Util.DT_HEADERS);
        receiveMessageRequest.setMessageAttributeNames(updatedMessageAttrNames);

        MessageConsumeParameters messageConsumeParameters = SqsV1Util.generateExternalConsumeMetrics(receiveMessageRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageConsumeParameters);
        return Weaver.callOriginal();
    }

    /*
     * Headers are used as inner classes to avoid class loading errors when running instrumentation tests
     * */

    public static class SQSRequestHeaders implements Headers {

        private SendMessageRequest request = null;

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
            if(messageAttributes != null) {
                MessageAttributeValue value = messageAttributes.get(name);
                if (value != null && value.getDataType().equalsIgnoreCase("string")) {
                    String valueString = value.getStringValue();
                    if (valueString != null) {
                        return valueString;
                    }
                }
            }

            Map<String, String> customRequestHeaders = request.getCustomRequestHeaders();
            if(customRequestHeaders != null) {
                return customRequestHeaders.get(name);
            }
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            List<String> list = new ArrayList<String>();
            String value = getHeader(name);
            if(value != null) {
                list.add(value);
            }
            return list;
        }

        @Override
        public void setHeader(String name, String value) {
            if(request != null) {
                Map<String, MessageAttributeValue> existingAttributes = request.getMessageAttributes();
                if(!existingAttributes.containsKey(name)) {
                    request.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
                }
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if(request != null) {
                Map<String, MessageAttributeValue> existingAttributes = request.getMessageAttributes();
                if(!existingAttributes.containsKey(name)) {
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

    public static class SQSBatchRequestHeaders implements Headers {

        private SendMessageBatchRequestEntry requestEntry = null;

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
            if(attributes != null) {
                MessageAttributeValue value = attributes.get(name);
                if(value != null) {
                    String dataType = value.getDataType();
                    if(dataType.equalsIgnoreCase("String")) {
                        String stringValue = value.getStringValue();
                        if(stringValue != null) return stringValue;
                    }
                }
            }
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            List<String> list = new ArrayList<String>();
            String value = getHeader(name);
            if(value != null && !value.isEmpty()) {
                list.add(value);
            }
            return list;
        }

        @Override
        public void setHeader(String name, String value) {
            if(requestEntry != null) {
                requestEntry.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if(requestEntry != null) {
                requestEntry.addMessageAttributesEntry(name, new MessageAttributeValue().withDataType("String").withStringValue(value));
            }
        }

        @Override
        public Collection<String> getHeaderNames() {
            if(requestEntry != null) {
                Map<String, MessageAttributeValue> attributes = requestEntry.getMessageAttributes();
                if(attributes != null) {
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

}
